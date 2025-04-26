package org.example.stockmarketsimulator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.example.stockmarketsimulator.model.Asset;
import org.example.stockmarketsimulator.repository.AssetsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/assets")
public class AssetsController {

    @Autowired
    private AssetsRepository assetsRepository;

    @Operation(summary = "Get all assets", description = "Retrieve a list of all assets.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of assets retrieved successfully", content = @Content(schema = @Schema(implementation = Asset.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping
    public List<Asset> getAssets() {
        return assetsRepository.findAll();
    }

    @Operation(summary = "Add a new asset", description = "Create a new asset.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Asset created successfully", content = @Content(schema = @Schema(implementation = Asset.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, missing required fields", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping
    public ResponseEntity<Asset> addAsset(@RequestBody Asset asset) {
        Asset savedAsset = assetsRepository.save(asset);
        return new ResponseEntity<>(savedAsset, HttpStatus.CREATED);
    }

    @Operation(summary = "Delete an asset", description = "Delete an asset by ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Asset deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Asset not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        Optional<Asset> asset = assetsRepository.findById(id);
        if (asset.isPresent()) {
            assetsRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
