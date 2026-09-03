package com.joseortiz.inventory.dto;

/**
 * Outcome of a successful import.
 *
 * @param fileName name of the processed file
 * @param importedRows rows inserted by this import
 * @param totalProducts rows in the inventory after the import
 */
public record ImportResultResponse(String fileName, int importedRows, long totalProducts) {}
