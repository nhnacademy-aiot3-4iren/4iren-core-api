package com.nhnacademy.environment.service;

import com.nhnacademy.environment.domain.RegionCoordinate;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RegionCoordinateService {
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
            log.info("Loaded {} KMA location coordinates", coordinateByNormalizedRegionName.size());
        } catch (IOException e) {
            throw new IllegalStateException("기상청 좌표 엑셀 파일을 읽을 수 없습니다.", e);
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
            throw new IllegalStateException("기상청 좌표 엑셀 파일을 탐색할 수 없습니다.", e);
        }
    }

    public RegionCoordinate findByRegionName(String regionName) {
        if (coordinateByNormalizedRegionName.isEmpty()) {
            throw new IllegalStateException("기상청 좌표 정보가 로드되지 않았습니다. 엑셀 파일 경로를 확인하세요.");
        }

        String normalizedRegionName = normalize(regionName);
        if (normalizedRegionName.isBlank()) {
            throw new IllegalArgumentException("지역명을 입력하세요.");
        }

        RegionCoordinate exactMatch = coordinateByNormalizedRegionName.get(normalizedRegionName);
        if (exactMatch != null) {
            return exactMatch;
        }

        List<RegionCoordinate> matches = selectBestMatches(regionName, findMatches(regionName));
        if (matches.isEmpty()) {
            matches = findUpperRegionMatches(regionName);
        }

        if (matches.size() == 1) {
            return matches.getFirst();
        }
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("지역 좌표를 찾을 수 없습니다: " + regionName);
        }

        String candidates = matches.stream()
                .map(RegionCoordinate::regionName)
                .limit(10)
                .collect(Collectors.joining(", "));
        throw new IllegalArgumentException("지역명이 모호합니다: " + regionName + " 후보: " + candidates);
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

    private List<RegionCoordinate> findUpperRegionMatches(String regionName) {
        List<String> keywords = splitKeywords(regionName);
        for (int size = keywords.size() - 1; size >= 2; size--) {
            String upperRegionName = String.join(" ", keywords.subList(0, size));
            List<RegionCoordinate> matches = selectBestMatches(upperRegionName, findMatches(upperRegionName));
            if (!matches.isEmpty()) {
                return matches;
            }
        }
        return List.of();
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
                .orElseThrow(() -> new IllegalStateException("좌표 엑셀 파일에서 헤더 행을 찾을 수 없습니다."));
        Map<String, Integer> headerIndexes = readHeaderIndexes(headerRow);

        int firstLevelIndex = findColumnIndex(headerIndexes, "1단계");
        int secondLevelIndex = findColumnIndex(headerIndexes, "2단계");
        int thirdLevelIndex = findColumnIndex(headerIndexes, "3단계");
        int nxIndex = findColumnIndex(headerIndexes, "격자X", "격자 X");
        int nyIndex = findColumnIndex(headerIndexes, "격자Y", "격자 Y");

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
            RegionCoordinate coordinate = new RegionCoordinate(regionName, nx, ny, levels.size());
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
        throw new IllegalStateException("좌표 엑셀 파일에서 컬럼을 찾을 수 없습니다: " + String.join(", ", candidates));
    }

    private String cellText(Row row, int columnIndex) {
        return dataFormatter.formatCellValue(row.getCell(columnIndex)).trim();
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("좌표 엑셀 파일에 빈 격자 좌표가 있습니다.");
        }
        return (int) Double.parseDouble(value.replace(",", ""));
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

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", "");
    }
}
