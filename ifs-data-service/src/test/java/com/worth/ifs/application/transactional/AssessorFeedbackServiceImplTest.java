package com.worth.ifs.application.transactional;

import com.worth.ifs.BaseServiceUnitTest;
import com.worth.ifs.BaseUnitTestMocksTest;
import com.worth.ifs.application.domain.Application;
import com.worth.ifs.commons.service.ServiceResult;
import com.worth.ifs.file.domain.FileEntry;
import com.worth.ifs.file.resource.FileEntryResource;
import com.worth.ifs.file.transactional.FileService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.InputStream;
import java.util.function.Supplier;

import static com.worth.ifs.application.builder.ApplicationBuilder.newApplication;
import static com.worth.ifs.commons.error.CommonErrors.notFoundError;
import static com.worth.ifs.commons.service.ServiceResult.serviceSuccess;
import static com.worth.ifs.file.domain.builders.FileEntryBuilder.newFileEntry;
import static com.worth.ifs.file.resource.builders.FileEntryResourceBuilder.newFileEntryResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AssessorFeedbackServiceImplTest extends BaseServiceUnitTest<AssessorFeedbackServiceImpl> {

    @Test
    public void testCreateAssessorFeedbackFileEntry() {

        FileEntryResource fileEntryToCreate = newFileEntryResource().build();
        Supplier<InputStream> inputStreamSupplier = () -> null;

        Application application = newApplication().withId(123L).build();
        when(applicationRepositoryMock.findOne(application.getId())).thenReturn(application);

        FileEntry createdFileEntry = newFileEntry().build();
        ServiceResult<Pair<File, FileEntry>> successfulFileCreationResult = serviceSuccess(Pair.of(new File("createdfile"), createdFileEntry));
        when(fileServiceMock.createFile(fileEntryToCreate, inputStreamSupplier)).thenReturn(successfulFileCreationResult);

        FileEntryResource createdFileEntryResource = newFileEntryResource().build();
        when(fileEntryMapper.mapToResource(createdFileEntry)).thenReturn(createdFileEntryResource);

        //
        // Call the method under test
        //
        ServiceResult<FileEntryResource> result = service.createAssessorFeedbackFileEntry(application.getId(), fileEntryToCreate, inputStreamSupplier);

        //
        // Assert that the result of our service call was successful and contains the resource returned from the mapper
        //
        assertTrue(result.isSuccess());
        assertEquals(createdFileEntryResource, result.getSuccessObject());

        verify(applicationRepositoryMock).findOne(application.getId());
        verifyNoMoreInteractions(addressRepositoryMock);
    }

    @Test
    public void testCreateAssessorFeedbackFileEntryButApplicationDoesntExist() {

        FileEntryResource fileEntryToCreate = newFileEntryResource().build();
        Supplier<InputStream> inputStreamSupplier = () -> null;

        when(applicationRepositoryMock.findOne(123L)).thenReturn(null);

        //
        // Call the method under test
        //
        ServiceResult<FileEntryResource> result = service.createAssessorFeedbackFileEntry(123L, fileEntryToCreate, inputStreamSupplier);

        //
        // Assert that the result of our service call was successful and contains the resource returned from the mapper
        //
        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(notFoundError(Application.class, 123L)));
    }


    @Override
    protected AssessorFeedbackServiceImpl supplyServiceUnderTest() {
        return new AssessorFeedbackServiceImpl();
    }
}
