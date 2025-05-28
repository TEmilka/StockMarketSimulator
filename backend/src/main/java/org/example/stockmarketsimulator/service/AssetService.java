package org.example.stockmarketsimulator.service;

import org.example.stockmarketsimulator.exception.BadRequestException;
import org.example.stockmarketsimulator.exception.ResourceNotFoundException;
import org.example.stockmarketsimulator.model.Asset;
import org.example.stockmarketsimulator.model.AssetPriceHistory;
import org.example.stockmarketsimulator.repository.AssetPriceHistoryRepository;
import org.example.stockmarketsimulator.repository.AssetsRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssetService {
    private final AssetsRepository assetsRepository;
    private final AssetPriceHistoryRepository assetPriceHistoryRepository;

    public AssetService(AssetsRepository assetsRepository,
                       AssetPriceHistoryRepository assetPriceHistoryRepository) {
        this.assetsRepository = assetsRepository;
        this.assetPriceHistoryRepository = assetPriceHistoryRepository;
    }

    public Map<String, Object> getAssets(String search, String sortBy, String sortDirection, Integer page, Integer size) {
        List<Asset> assets = assetsRepository.findAll();

        assets = filterAssets(assets, search);
        assets = sortAssets(assets, sortBy, sortDirection);

        if (page == null || size == null) {
            return Map.of("content", assets);
        }

        return paginateAssets(assets, page, size);
    }

    private List<Asset> filterAssets(List<Asset> assets, String search) {
        if (search == null || search.isEmpty()) {
            return assets;
        }

        String searchLower = search.toLowerCase();
        return assets.stream()
                .filter(asset ->
                        asset.getSymbol().toLowerCase().contains(searchLower) ||
                        asset.getName().toLowerCase().contains(searchLower))
                .collect(Collectors.toList());
    }

    private List<Asset> sortAssets(List<Asset> assets, String sortBy, String sortDirection) {
        if (sortBy == null) {
            return assets;
        }

        boolean asc = sortDirection == null || sortDirection.equalsIgnoreCase("asc");
        Comparator<Asset> comparator = getComparator(sortBy);
        
        if (!asc) {
            comparator = comparator.reversed();
        }

        return assets.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private Comparator<Asset> getComparator(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "price" -> Comparator.comparing(Asset::getPrice);
            case "name" -> Comparator.comparing(Asset::getName, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(Asset::getId);
        };
    }

    private Map<String, Object> paginateAssets(List<Asset> assets, int page, int size) {
        int fromIndex = Math.min(page * size, assets.size());
        int toIndex = Math.min(fromIndex + size, assets.size());
        List<Asset> pagedAssets = assets.subList(fromIndex, toIndex);

        Map<String, Object> response = new HashMap<>();
        response.put("content", pagedAssets);
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", assets.size());
        response.put("totalPages", (int) Math.ceil((double) assets.size() / size));

        return response;
    }

    public Asset createAsset(Asset asset) {
        validateAsset(asset);
        return assetsRepository.save(asset);
    }

    private void validateAsset(Asset asset) {
        if (asset.getName() == null || asset.getSymbol() == null) {
            throw new BadRequestException("Nazwa i symbol aktywa są wymagane.");
        }
    }

    public void deleteAsset(Long id) {
        if (!assetsRepository.existsById(id)) {
            throw new ResourceNotFoundException("Aktywo o ID " + id + " nie zostało znalezione.");
        }
        assetsRepository.deleteById(id);
    }

    public List<Map<String, Object>> getAssetHistory(Long id) {
        Asset asset = assetsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aktywo nie zostało znalezione"));

        List<AssetPriceHistory> historyList = assetPriceHistoryRepository
                .findTop30ByAssetOrderByTimestampDesc(asset);
        
        historyList.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));

        return historyList.stream()
                .map(h -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("timestamp", h.getTimestamp().toString());
                    point.put("price", h.getPrice());
                    return point;
                })
                .collect(Collectors.toList());
    }
}
