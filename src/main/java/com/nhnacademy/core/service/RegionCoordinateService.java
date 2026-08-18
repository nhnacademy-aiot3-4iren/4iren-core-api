package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.GeoCoordinate;
import com.nhnacademy.core.domain.RegionCoordinate;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.InvalidRequestException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.exception.ServiceUnavailableException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegionCoordinateService {
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private final KakaoGeocodingService kakaoGeocodingService;
    private final DataFormatter dataFormatter = new DataFormatter();
    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    @Value("${kma.location-coordinate-file:classpath:동네예보지점좌표(위경도)_260701.xlsx}")
    private Resource coordinateFile;

    private Map<String, RegionCoordinate> coordinateByNormalizedRegionName = Map.of();

    @PostConstruct
    void loadCoordinateFile() {
        Resource resolvedCoordinateFile = resolveCoordinateFile();
        if (resolvedCoordinateFile == null) {
            log.warn("KMA location coordinate file does not exist: {}", coordinateFile);
            return;
        }

        ZipSecureFile.setMinInflateRatio(0.001);
        try (InputStream inputStream = resolvedCoordinateFile.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, RegionCoordinate> coordinates = readCoordinates(sheet);
            coordinateByNormalizedRegionName = Map.copyOf(coordinates);
            log.info("기상청 제공 지역 좌표 로드: {}건", coordinateByNormalizedRegionName.size());
        } catch (IOException e) {
            throw new ServiceUnavailableException(
                    ErrorCode.KMA_COORDINATE_FILE_UNAVAILABLE,
                    Map.of("coordinateFile", String.valueOf(coordinateFile)),
                    e
            );
        }
    }

    private Resource resolveCoordinateFile() {
        if (coordinateFile.exists()) {
            return coordinateFile;
        }

        try {
            Resource[] xlsxResources = resourcePatternResolver.getResources("classpath*:*.xlsx");
            return Arrays.stream(xlsxResources)
                    .filter(Resource::exists)
                    .filter(resource -> {
                        String filename = resource.getFilename();
                        return filename != null && filename.contains("260701");
                    })
                    .findFirst()
                    .orElseGet(() -> Arrays.stream(xlsxResources)
                            .filter(Resource::exists)
                            .findFirst()
                            .orElse(null));
        } catch (IOException e) {
            throw new ServiceUnavailableException(
                    ErrorCode.KMA_COORDINATE_FILE_UNAVAILABLE,
                    Map.of("coordinateFile", String.valueOf(coordinateFile)),
                    e
            );
        }
    }

    public RegionCoordinate findByRegionName(String regionName) {
        if (coordinateByNormalizedRegionName.isEmpty()) {
            throw new ServiceUnavailableException(
                    ErrorCode.KMA_COORDINATE_FILE_UNAVAILABLE,
                    Map.of("coordinateFile", String.valueOf(coordinateFile))
            );
        }

        String normalizedRegionName = normalize(regionName);
        if (normalizedRegionName.isBlank()) {
            throw new InvalidRequestException(ErrorCode.KMA_REGION_NAME_REQUIRED);
        }

        RegionCoordinate exactMatch = coordinateByNormalizedRegionName.get(normalizedRegionName);
        if (exactMatch != null) {
            return exactMatch;
        }

        List<RegionCoordinate> matches = selectBestMatches(regionName, findMatches(regionName));
        if (matches.isEmpty()) {
            Optional<RegionCoordinate> nearestMatch = kakaoGeocodingService.geocode(regionName)
                    .flatMap(this::findNearestByCoordinate);
            if (nearestMatch.isPresent()) {
                RegionCoordinate coordinate = nearestMatch.get();
                log.info("지역명 좌표 매칭 실패, 지오코딩 기반 가장 가까운 기상청 좌표 사용: {} -> {}", regionName, coordinate.regionName());
                return coordinate;
            }
        }

        if (matches.size() == 1) {
            return matches.getFirst();
        }
        if (matches.isEmpty()) {
            throw new ResourceNotFoundException(
                    ErrorCode.KMA_REGION_COORDINATE_NOT_FOUND,
                    Map.of("regionName", regionName)
            );
        }

        String candidates = matches.stream()
                .map(RegionCoordinate::regionName)
                .limit(10)
                .collect(Collectors.joining(", "));
        throw new InvalidRequestException(
                ErrorCode.KMA_REGION_NAME_AMBIGUOUS,
                Map.of(
                        "regionName", regionName,
                        "candidates", candidates
                )
        );
    }

    private List<RegionCoordinate> findMatches(String regionName) {
        String normalizedRegionName = normalize(regionName);
        List<String> keywords = splitKeywords(regionName);
        return coordinateByNormalizedRegionName.entrySet().stream()
                .filter(entry -> isMatched(entry.getKey(), normalizedRegionName, keywords))
                .map(Map.Entry::getValue)
                .distinct()
                .toList();
    }

    private List<RegionCoordinate> selectBestMatches(String regionName, List<RegionCoordinate> matches) {
        if (matches.size() <= 1) {
            return matches;
        }

        int inputLevelCount = splitKeywords(regionName).size();
        List<RegionCoordinate> sameLevelMatches = matches.stream()
                .filter(coordinate -> coordinate.levelCount() == inputLevelCount)
                .toList();
        if (!sameLevelMatches.isEmpty()) {
            return sameLevelMatches;
        }

        return matches;
    }

    private Map<String, RegionCoordinate> readCoordinates(Sheet sheet) {
        Row headerRow = findHeaderRow(sheet)
                .orElseThrow(() -> coordinateFileException("headerRow", "헤더 행"));
        Map<String, Integer> headerIndexes = readHeaderIndexes(headerRow);

        int firstLevelIndex = findColumnIndex(headerIndexes, "1단계");
        int secondLevelIndex = findColumnIndex(headerIndexes, "2단계");
        int thirdLevelIndex = findColumnIndex(headerIndexes, "3단계");
        int nxIndex = findColumnIndex(headerIndexes, "격자X", "격자 X");
        int nyIndex = findColumnIndex(headerIndexes, "격자Y", "격자 Y");
        int longitudeIndex = findColumnIndex(headerIndexes, "경도", "경도(초/100)", "longitude", "lon");
        int latitudeIndex = findColumnIndex(headerIndexes, "위도", "위도(초/100)", "latitude", "lat");

        Map<String, RegionCoordinate> coordinates = new LinkedHashMap<>();
        for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            String firstLevel = cellText(row, firstLevelIndex);
            String secondLevel = cellText(row, secondLevelIndex);
            String thirdLevel = cellText(row, thirdLevelIndex);
            List<String> levels = readLevels(firstLevel, secondLevel, thirdLevel);
            String regionName = String.join(" ", levels);
            if (regionName.isBlank()) {
                continue;
            }

            Integer nx = parseInteger(cellText(row, nxIndex));
            Integer ny = parseInteger(cellText(row, nyIndex));
            Double longitude = parseDouble(cellText(row, longitudeIndex));
            Double latitude = parseDouble(cellText(row, latitudeIndex));
            RegionCoordinate coordinate = new RegionCoordinate(regionName, nx, ny, longitude, latitude, levels.size());
            coordinates.putIfAbsent(normalize(regionName), coordinate);
        }

        return coordinates;
    }

    private Optional<Row> findHeaderRow(Sheet sheet) {
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= Math.min(sheet.getLastRowNum(), 20); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            Map<String, Integer> headerIndexes = readHeaderIndexes(row);
            if (headerIndexes.containsKey(normalize("1단계")) && headerIndexes.containsKey(normalize("격자X"))) {
                return Optional.of(row);
            }
        }
        return Optional.empty();
    }

    private Map<String, Integer> readHeaderIndexes(Row row) {
        Map<String, Integer> headerIndexes = new HashMap<>();
        for (Cell cell : row) {
            String headerName = normalize(dataFormatter.formatCellValue(cell));
            if (!headerName.isBlank()) {
                headerIndexes.put(headerName, cell.getColumnIndex());
            }
        }
        return headerIndexes;
    }

    private int findColumnIndex(Map<String, Integer> headerIndexes, String... candidates) {
        for (String candidate : candidates) {
            Integer columnIndex = headerIndexes.get(normalize(candidate));
            if (columnIndex != null) {
                return columnIndex;
            }
        }
        throw coordinateFileException("missingColumns", String.join(", ", candidates));
    }

    private String cellText(Row row, int columnIndex) {
        return dataFormatter.formatCellValue(row.getCell(columnIndex)).trim();
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            throw coordinateFileException("gridCoordinate", "격자 좌표");
        }
        try {
            return (int) Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException e) {
            throw coordinateFileException("gridCoordinate", value, e);
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            throw coordinateFileException("geoCoordinate", "위경도 좌표");
        }
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException e) {
            throw coordinateFileException("geoCoordinate", value, e);
        }
    }

    private ServiceUnavailableException coordinateFileException(String field, Object value) {
        return coordinateFileException(field, value, null);
    }

    private ServiceUnavailableException coordinateFileException(String field, Object value, Throwable cause) {
        return new ServiceUnavailableException(
                ErrorCode.KMA_COORDINATE_FILE_UNAVAILABLE,
                Map.of(
                        "coordinateFile", String.valueOf(coordinateFile),
                        field, String.valueOf(value)
                ),
                cause
        );
    }

    private List<String> readLevels(String... levels) {
        List<String> names = new ArrayList<>();
        for (String level : levels) {
            if (level != null && !level.isBlank()) {
                names.add(level.trim());
            }
        }
        return names;
    }

    private List<String> splitKeywords(String value) {
        return Arrays.stream(value.trim().split("\\s+"))
                .map(this::normalize)
                .filter(keyword -> !keyword.isBlank())
                .toList();
    }

    private boolean isMatched(String regionName, String normalizedRegionName, List<String> keywords) {
        if (regionName.contains(normalizedRegionName)) {
            return true;
        }
        return keywords.stream().allMatch(regionName::contains);
    }

    private Optional<RegionCoordinate> findNearestByCoordinate(GeoCoordinate coordinate) {
        return coordinateByNormalizedRegionName.values().stream()
                .distinct()
                .filter(regionCoordinate -> regionCoordinate.longitude() != null && regionCoordinate.latitude() != null)
                .min(Comparator.comparingDouble(regionCoordinate -> distanceKm(coordinate, regionCoordinate)));
    }

    private double distanceKm(GeoCoordinate source, RegionCoordinate target) {
        double sourceLatitude = Math.toRadians(source.latitude());
        double targetLatitude = Math.toRadians(target.latitude());
        double deltaLatitude = targetLatitude - sourceLatitude;
        double deltaLongitude = Math.toRadians(target.longitude() - source.longitude());

        double haversine = Math.sin(deltaLatitude / 2) * Math.sin(deltaLatitude / 2)
                + Math.cos(sourceLatitude) * Math.cos(targetLatitude)
                * Math.sin(deltaLongitude / 2) * Math.sin(deltaLongitude / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(haversine));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", "");
    }
}
