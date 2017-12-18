package org.innovateuk.ifs.application.controller;

import org.innovateuk.ifs.commons.rest.RestResult;
import org.innovateuk.ifs.file.resource.FileEntryResource;
import org.innovateuk.ifs.file.service.FilesizeAndTypeFileValidator;
import org.innovateuk.ifs.finance.transactional.OverheadFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

import static org.innovateuk.ifs.file.controller.FileControllerUtils.handleFileDownload;
import static org.innovateuk.ifs.file.controller.FileControllerUtils.handleFileUpload;

/**
 * Controller for handling the overheads calculation spreadsheet file upload
 */
@RestController
@RequestMapping("/overheadcalculation")
public class OverheadFileController {

    @Value("${ifs.data.service.file.storage.overheadcalculation.max.filesize.bytes}")
    private Long maxFilesizeBytesForOverheadCalculation;

    @Value("${ifs.data.service.file.storage.overheadcalculation.valid.media.types}")
    private List<String> validMediaTypesForOverheadCalculation;

    @Autowired
    private OverheadFileService overheadFileService;

    @Autowired
    @Qualifier("mediaTypeStringsFileValidator")
    private FilesizeAndTypeFileValidator<List<String>> fileValidator;

    @GetMapping(value = "/overheadCalculationDocumentDetails", produces = "application/json")
    public RestResult<FileEntryResource> getFileDetails(
            @RequestParam(value = "overheadId") long overheadId) {

        return overheadFileService.getFileEntryDetails(overheadId).toGetResponse();
    }

    @GetMapping(value = "/projectOverheadCalculationDocumentDetails", produces = "application/json")
    public RestResult<FileEntryResource> getProjectFileDetails(
            @RequestParam(value = "overheadId") long overheadId) {

        return overheadFileService.getProjectFileEntryDetails(overheadId).toGetResponse();
    }

    @GetMapping("/overheadCalculationDocument")
    public @ResponseBody
    ResponseEntity<Object> getFileContents(
            @RequestParam("overheadId") long overheadId) throws IOException {

        return handleFileDownload(() -> overheadFileService.getFileEntryContents(overheadId));
    }

    @GetMapping("/projectOverheadCalculationDocument")
    public @ResponseBody
    ResponseEntity<Object> getProjectFileContents(
            @RequestParam("overheadId") long overheadId) throws IOException {
        return handleFileDownload(() -> overheadFileService.getProjectFileEntryContents(overheadId));
    }

    @PostMapping(value = "/overheadCalculationDocument", produces = "application/json")
    public RestResult<FileEntryResource> createCalculationFile(
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            @RequestHeader(value = "Content-Length", required = false) String contentLength,
            @RequestParam(value = "overheadId") long overheadId,
            @RequestParam(value = "filename", required = false) String originalFilename,
            HttpServletRequest request) {

        return handleFileUpload(contentType, contentLength, originalFilename, fileValidator, validMediaTypesForOverheadCalculation, maxFilesizeBytesForOverheadCalculation, request, (fileAttributes, inputStreamSupplier) ->
                overheadFileService.createFileEntry(overheadId, fileAttributes.toFileEntryResource(), inputStreamSupplier));
    }

    @PutMapping(value = "/overheadCalculationDocument", produces = "application/json")
    public RestResult<FileEntryResource> updateCalculationFile(
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            @RequestHeader(value = "Content-Length", required = false) String contentLength,
            @RequestParam(value = "overheadId") long overheadId,
            @RequestParam(value = "filename", required = false) String originalFilename,
            HttpServletRequest request) {

        return handleFileUpload(contentType, contentLength, originalFilename, fileValidator, validMediaTypesForOverheadCalculation, maxFilesizeBytesForOverheadCalculation, request, (fileAttributes, inputStreamSupplier) ->
                overheadFileService.updateFileEntry(overheadId, fileAttributes.toFileEntryResource(), inputStreamSupplier));
    }

    @DeleteMapping(value = "/overheadCalculationDocument", produces = "application/json")
    public RestResult<Void> deleteCalculationFile(
            @RequestParam(value = "overheadId") long overheadId) throws IOException {

        return overheadFileService.deleteFileEntry(overheadId).toGetResponse();
    }
}