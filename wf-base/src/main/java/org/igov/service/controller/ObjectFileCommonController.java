package org.igov.service.controller;

import org.igov.model.action.task.core.entity.AttachmentEntityI;
import org.igov.service.exception.FileServiceIOException;
import org.igov.service.exception.CommonServiceException;
import com.google.common.base.Charsets;
import io.swagger.annotations.*;
import org.activiti.engine.*;
import org.activiti.engine.history.*;
import org.activiti.engine.task.*;
import org.activiti.rest.service.api.runtime.process.ExecutionBaseResource;
import org.apache.commons.io.IOUtils;
import org.igov.service.business.action.event.HistoryEventService;
import org.igov.service.business.action.task.core.AbstractModelTask;
import org.igov.model.action.task.core.BuilderAttachModelCover;
import org.igov.util.convert.ByteArrayMultipartFileOld;
import org.igov.service.business.action.task.systemtask.FileTaskUpload;
import org.igov.io.GeneralConfig;
import org.igov.io.bankid.BankIDConfig;
import org.igov.io.bankid.BankIDUtils;
import org.igov.io.db.kv.temp.IBytesDataInmemoryStorage;
import org.igov.io.db.kv.temp.exception.RecordInmemoryException;
import org.igov.model.action.task.core.AttachmentCover;
import org.igov.util.Util;
import org.igov.util.convert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.igov.service.business.action.task.core.AbstractModelTask.getByteArrayMultipartFileFromStorageInmemory;
import org.igov.service.business.action.task.core.ActionTaskService;
import org.igov.io.db.kv.temp.model.ByteArrayMultipartFile;
import org.igov.model.action.task.core.entity.AttachmentEntity;
import org.springframework.http.ResponseEntity;

//import com.google.common.base.Optional;

/**
 * @author BW
 */

@Controller
@Api(tags = { "ObjectFileCommonController" }, description = "Обьекты файлов общие")
@RequestMapping(value = "/object/file")
public class ObjectFileCommonController extends ExecutionBaseResource {
    
    private static final Logger LOG = LoggerFactory
            .getLogger(ObjectFileCommonController.class);

    @Autowired
    private TaskService taskService;
    //@Autowired
    //private ExceptionCommonController exceptionController;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private HistoryService historyService;
    //@Autowired
    //private FormService formService;
    //@Autowired
    //private FlowSlotTicketDao flowSlotTicketDao;
    
    //@Autowired
    //private TaskService taskService;
    //@Autowired
    //private RepositoryService repositoryService;
    @Autowired
    private IBytesDataInmemoryStorage oBytesDataInmemoryStorage;
    //@Autowired
    //private HistoryService historyService;
    //@Autowired
    //private HistoryEventService historyEventService;
    @Autowired
    private IdentityService identityService;
    //@Autowired
    //private FormService formService;
    @Autowired
    private GeneralConfig generalConfig;
    @Autowired
    private BankIDConfig bankIDConfig;

    
    /**
     * Укладываем в редис multipartFileToByteArray
     *
     * @param file
     * @return attachId
     * @throws org.igov.service.exception.FileServiceIOException
     */
    @ApiOperation(value = "PutAttachmentsToRedis", notes = "#####  ObjectFileCommonController: описания нет #####\n\n")
    @RequestMapping(value = "/upload_file_to_redis", method = RequestMethod.POST)
    @Transactional
    public
    @ResponseBody
    String putAttachmentsToRedis(
            @RequestParam(required = true, value = "file") MultipartFile file)
            throws FileServiceIOException {
        try {
            String key = oBytesDataInmemoryStorage.putBytes(AbstractModelTask
                    .multipartFileToByteArray(file, file.getOriginalFilename())
                    .toByteArray());
            return key;
        } catch (RecordInmemoryException | IOException e) {
            LOG.warn(e.getMessage(), e);
            throw new FileServiceIOException(
                    FileServiceIOException.Error.REDIS_ERROR, e.getMessage());
        }
    }

    @ApiOperation(value = "GetAttachmentsFromRedis", notes = "#####  ObjectFileCommonController: описания нет #####\n\n")
    @RequestMapping(value = "/download_file_from_redis", method = RequestMethod.GET)
    @Transactional
    public
    @ResponseBody
    byte[] getAttachmentsFromRedis(
            @RequestParam("key") String key) throws FileServiceIOException {
        byte[] upload = null;
        try {
            upload = oBytesDataInmemoryStorage.getBytes(key);
        } catch (RecordInmemoryException e) {
            LOG.warn(e.getMessage(), e);
            throw new FileServiceIOException(
                    FileServiceIOException.Error.REDIS_ERROR, e.getMessage());
        }
        return upload;
    }

    @ApiOperation(value = "GetAttachmentsFromRedisBytes", notes = "#####  ObjectFileCommonController: описания нет #####\n\n")
    @RequestMapping(value = "/download_file_from_redis_bytes", method = RequestMethod.GET)
    @Transactional
    public
    @ResponseBody
    byte[] getAttachmentsFromRedisBytes(
            @RequestParam("key") String key) throws FileServiceIOException {
        byte[] upload = null;
        try {
            byte[] aByteFile = oBytesDataInmemoryStorage.getBytes(key);
            ByteArrayMultipartFile oByteArrayMultipartFile = null;
            oByteArrayMultipartFile = getByteArrayMultipartFileFromStorageInmemory(aByteFile);

            if (oByteArrayMultipartFile != null) {

                upload = oByteArrayMultipartFile.getBytes();

            } else {
                // LOG.error("[getAttachmentsFromRedisBytes]oByteArrayMultipartFile==null! aByteFile="
                // + aByteFile.
                // .toString());
                // Unreachable code?
                LOG.error("[getAttachmentsFromRedisBytes]oByteArrayMultipartFile==null! key="
                        + key);
            }

        } catch (RecordInmemoryException e) {
            LOG.warn(e.getMessage(), e);
            throw new FileServiceIOException(
                    FileServiceIOException.Error.REDIS_ERROR, e.getMessage());
        } catch (ClassNotFoundException | IOException e) {
            LOG.error(e.getMessage(), e);
            throw new ActivitiException(e.getMessage(), e);
        }

        return upload;
    }

    @ApiOperation(value = "Проверка ЭЦП на файле хранящемся в Redis", notes = "#####  ObjectFileCommonController: Проверка ЭЦП на файле хранящемся в Redis #####\n\n"
            + "HTTP Context: https://test.region.igov.org.ua/wf/service/object/file/check_file_from_redis_sign?sID_File_Redis=sID_File_Redis\n\n\n"
            + "возвращает json объект описывающий ЭЦП файла.\n\n"
            + "Примеры:\n\n"
            + "https://test.region.igov.org.ua/wf/service/object/file/check_file_from_redis_sign?sID_File_Redis=d2993755-70e5-409e-85e5-46ba8ce98e1d\n\n"
            + "Ответ json описывающий ЭЦП:\n\n"
            + "\n```json\n"
            + "{\n"
            + "  \"state\": \"ok\",\n"
            + "  \"customer\": {\n"
            + "    \"inn\": \"1436057000\",\n"
            + "    \"fullName\": \"Сервіс зберігання сканкопій\",\n"
            + "    \"signatureData\": {\n"
            + "      \"name\": \"АЦСК ПАТ КБ «ПРИВАТБАНК»\",\n"
            + "      \"serialNumber\": \"0D84EDA1BB9381E80400000079DD02004A710800\",\n"
            + "      \"timestamp\": \"29.10.2015 13:45:33\",\n"
            + "      \"code\": true,\n"
            + "      \"desc\": \"ПІДПИС ВІРНИЙ\",\n"
            + "      \"dateFrom\": \"13.08.2015 11:24:31\",\n"
            + "      \"dateTo\": \"12.08.2016 23:59:59\",\n"
            + "      \"sn\": \"UA-14360570-1\"\n"
            + "    },\n"
            + "    \"organizations\": [\n"
            + "      {\n"
            + "        \"type\": \"edsOwner\",\n"
            + "        \"name\": \"ПАТ КБ «ПРИВАТБАНК»\",\n"
            + "        \"mfo\": \"14360570\",\n"
            + "        \"position\": \"Технологічний сертифікат\",\n"
            + "        \"ownerDesc\": \"Співробітник банку\",\n"
            + "        \"address\": {\n"
            + "          \"type\": \"factual\",\n"
            + "          \"state\": \"Дніпропетровська\",\n"
            + "          \"city\": \"Дніпропетровськ\"\n"
            + "        }\n"
            + "      },\n"
            + "      {\n"
            + "        \"type\": \"edsIsuer\",\n"
            + "        \"name\": \"ПУБЛІЧНЕ АКЦІОНЕРНЕ ТОВАРИСТВО КОМЕРЦІЙНИЙ БАНК «ПРИВАТБАНК»\",\n"
            + "        \"unit\": \"АЦСК\",\n"
            + "        \"address\": {\n"
            + "          \"type\": \"factual\",\n"
            + "          \"state\": \"Дніпропетровська\",\n"
            + "          \"city\": \"Дніпропетровськ\"\n"
            + "        }\n"
            + "      }\n"
            + "    ]\n"
            + "  }\n"
            + "}\n"
            + "\n```\n"
            + "Ответ для несуществующего ключа (sID_File_Redis):\n"
            + "\n```json\n"
            + "{\"code\":\"SYSTEM_ERR\",\"message\":\"File with sID_File_Redis 'd2993755-70e5-409e-85e5-46ba8ce98e1e' not found.\"}\n\n"
            + "\n```\n"
            + "Ответ для файла который не имеет наложеной ЭЦП:\n\n"
            + "\n```json\n"
            + "{}\n"
            + "\n```\n")
    @RequestMapping(value = "/check_file_from_redis_sign", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @Transactional
    public
    @ResponseBody
    String checkAttachmentsFromRedisSign(
            @ApiParam(value = "key по которому можно получить файл из хранилища Redis", required = true) @RequestParam("sID_File_Redis") String sID_File_Redis)
            throws FileServiceIOException {
        byte[] upload = null;
        String fileName = null;
        try {
            byte[] aByteFile = oBytesDataInmemoryStorage.getBytes(sID_File_Redis);
            ByteArrayMultipartFile oByteArrayMultipartFile = null;

            if (aByteFile == null) {
                throw new ActivitiObjectNotFoundException(
                        "File with sID_File_Redis '" + sID_File_Redis
                                + "' not found.");
            }
            try {
                oByteArrayMultipartFile = getByteArrayMultipartFileFromStorageInmemory(aByteFile);
            } catch (ClassNotFoundException | IOException e1) {
                throw new ActivitiException(e1.getMessage(), e1);
            }
            if (oByteArrayMultipartFile != null) {

                upload = oByteArrayMultipartFile.getBytes();
                fileName = oByteArrayMultipartFile.getName();

            } else {
                // /
                // LOG.error("[checkAttachmentsFromRedisSign]oByteArrayMultipartFile==null! aByteFile="
                // + aByteFile
                // / .toString());
                // Unreachable code?
                LOG.error("[checkAttachmentsFromRedisSign]oByteArrayMultipartFile==null! sID_File_Redis="
                        + sID_File_Redis);
            }

        } catch (RecordInmemoryException e) {
            LOG.warn(e.getMessage(), e);
            throw new FileServiceIOException(
                    FileServiceIOException.Error.REDIS_ERROR, e.getMessage());
        }

        String soSignData = BankIDUtils.checkECP(bankIDConfig.sClientId(),
                bankIDConfig.sClientSecret(), generalConfig.sHostCentral(),
                upload, fileName);

        return soSignData;
    }

    /**
     * Получение Attachment средствами активити из таблицы ACT_HI_ATTACHMENT
     *
     * @param taskId
     * @param attachmentId
     * @param nFile
     * @param httpResponse
     * @return
     * @throws java.io.IOException
     */
    @ApiOperation(value = "Загрузки прикрепленного к заявке файла из постоянной базы", notes = "#####  ObjectFileCommonController: Загрузки прикрепленного к заявке файла из постоянной базы #####\n\n"
            + "HTTP Context: https://server:port/wf/service/object/file/download_file_from_db?taskId=XXX&attachmentId=XXX&nFile=XXX\n\n\n"
            + "Пример:\n https://test.igov.org.ua/wf/service/object/file/download_file_from_db?taskId=82596&attachmentId=6726532&nFile=7\n")
    @RequestMapping(value = "/download_file_from_db", method = RequestMethod.GET)
    @Transactional
    public
    @ResponseBody
    byte[] getAttachmentFromDb(
            @ApiParam(value = "ид задачи", required = true) @RequestParam(value = "taskId") String taskId,
            @ApiParam(value = "ID прикрепленного файла", required = false) @RequestParam(required = false, value = "attachmentId") String attachmentId,
            @ApiParam(value = "порядковый номер прикрепленного файла", required = false) @RequestParam(required = false, value = "nFile") Integer nFile,
            HttpServletResponse httpResponse) throws IOException {

        ActionTaskService oManagerActiviti=new ActionTaskService();
        
        // Получаем по задаче ид процесса
        HistoricTaskInstance historicTaskInstanceQuery = historyService
                .createHistoricTaskInstanceQuery().taskId(taskId)
                .singleResult();
        String processInstanceId = historicTaskInstanceQuery
                .getProcessInstanceId();
        if (processInstanceId == null) {
            throw new ActivitiObjectNotFoundException(
                    "ProcessInstanceId for taskId '" + taskId + "' not found.",
                    Attachment.class);
        }

        // Выбираем по процессу прикрепленные файлы
        Attachment attachmentRequested = oManagerActiviti.getAttachment(attachmentId, taskId,
                nFile, processInstanceId);

        InputStream attachmentStream = taskService
                .getAttachmentContent(attachmentRequested.getId());
        if (attachmentStream == null) {
            throw new ActivitiObjectNotFoundException("Attachment for taskId '"
                    + taskId + "' doesn't have content associated with it.",
                    Attachment.class);
        }

        String sFileName = attachmentRequested.getName();
        int nTo = sFileName.lastIndexOf(".");
        if (nTo >= 0) {
            sFileName = "attach_" + attachmentRequested.getId() + "."
                    + sFileName.substring(nTo + 1);
        }

        // Вычитывем из потока массив байтов контента и помещаем параметры
        // контента в header
        ByteArrayMultipartFileOld multipartFile = new ByteArrayMultipartFileOld(
                attachmentStream, attachmentRequested.getDescription(),
                sFileName, attachmentRequested.getType());
        httpResponse.setHeader("Content-disposition", "attachment; filename="
                + sFileName);
        httpResponse.setHeader("Content-Type", "application/octet-stream");

        httpResponse.setContentLength(multipartFile.getBytes().length);

        return multipartFile.getBytes();
    }

    /**
     * @param taskId       id таски Activiti BP
     * @param attachmentId id атачмента приложеного к таске
     */
    @ApiOperation(value = "Проверка ЭЦП на атачменте(файл) таски Activiti", notes = "#####  ObjectFileCommonController: Проверка ЭЦП на атачменте(файл) таски Activiti #####\n\n"
            + "HTTP Context: https://test.region.igov.org.ua/wf/service/object/file/check_attachment_sign?nID_Task=nID_Task&nID_Attach=nID_Attach]\n\n"
            + "возвращает json объект описывающий ЭЦП файла-аттачмента.\n\n"
            + "Примеры:\n\n"
            + "https://test.region.igov.org.ua/wf/service/object/file/check_attachment_sign?nID_Task=7315073&nID_Attach=7315075\n"
            + "Ответ:\n"
            + "\n```json\n"
            + "{\n"
            + "  \"state\": \"ok\",\n"
            + "  \"customer\": {\n"
            + "    \"inn\": \"1436057000\",\n"
            + "    \"fullName\": \"Сервіс зберігання сканкопій\",\n"
            + "    \"signatureData\": {\n"
            + "      \"name\": \"АЦСК ПАТ КБ «ПРИВАТБАНК»\",\n"
            + "      \"serialNumber\": \"0D84EDA1BB9381E80400000079DD02004A710800\",\n"
            + "      \"timestamp\": \"29.10.2015 13:45:33\",\n"
            + "      \"code\": true,\n"
            + "      \"desc\": \"ПІДПИС ВІРНИЙ\",\n"
            + "      \"dateFrom\": \"13.08.2015 11:24:31\",\n"
            + "      \"dateTo\": \"12.08.2016 23:59:59\",\n"
            + "      \"sn\": \"UA-14360570-1\"\n"
            + "    },\n"
            + "    \"organizations\": [\n"
            + "      {\n"
            + "        \"type\": \"edsOwner\",\n"
            + "        \"name\": \"ПАТ КБ «ПРИВАТБАНК»\",\n"
            + "        \"mfo\": \"14360570\",\n"
            + "        \"position\": \"Технологічний сертифікат\",\n"
            + "        \"ownerDesc\": \"Співробітник банку\",\n"
            + "        \"address\": {\n"
            + "          \"type\": \"factual\",\n"
            + "          \"state\": \"Дніпропетровська\",\n"
            + "          \"city\": \"Дніпропетровськ\"\n"
            + "        }\n"
            + "      },\n"
            + "      {\n"
            + "        \"type\": \"edsIsuer\",\n"
            + "        \"name\": \"ПУБЛІЧНЕ АКЦІОНЕРНЕ ТОВАРИСТВО КОМЕРЦІЙНИЙ БАНК «ПРИВАТБАНК»\",\n"
            + "        \"unit\": \"АЦСК\",\n"
            + "        \"address\": {\n"
            + "          \"type\": \"factual\",\n"
            + "          \"state\": \"Дніпропетровська\",\n"
            + "          \"city\": \"Дніпропетровськ\"\n"
            + "        }\n"
            + "      }\n"
            + "    ]\n"
            + "  }\n"
            + "}\n"
            + "\n```\n"
            + "\nОтвет для несуществующей таски (nID_Task):\n"
            + "\n```json\n"
            + "{\"code\":\"SYSTEM_ERR\",\"message\":\"ProcessInstanceId for taskId '7315070' not found.\"}\n"
            + "\n```\n"
            + "\nОтвет для несуществующего атачмента (nID_Attach):\n"
            + "\n```json\n"
            + "{\"code\":\"SYSTEM_ERR\",\"message\":\"Attachment for taskId '7315073' not found.\"}\n"
            + "\n```\n"
            + "\nОтвет для атачмента который не имеет наложеной ЭЦП:\n"
            + "\n```json\n"
            + "{}\n"
            + "\n```\n")
    @RequestMapping(value = "/check_attachment_sign", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @Transactional
    public
    @ResponseBody
    String checkAttachSign(
            @ApiParam(value = "ИД-номер таски", required = true) @RequestParam(value = "nID_Task") String taskId,
            @ApiParam(value = "id атачмента приложеного к таске", required = true) @RequestParam(value = "nID_Attach") String attachmentId)
            throws IOException {

        HistoricTaskInstance historicTaskInstanceQuery = historyService
                .createHistoricTaskInstanceQuery().taskId(taskId)
                .singleResult();
        String processInstanceId = null;

        if (historicTaskInstanceQuery != null) {
            processInstanceId = historicTaskInstanceQuery
                    .getProcessInstanceId();
        }
        if (processInstanceId == null) {
            throw new ActivitiObjectNotFoundException(
                    "ProcessInstanceId for taskId '" + taskId + "' not found.",
                    Attachment.class);
        }

        ActionTaskService oManagerActiviti=new ActionTaskService();
        
        Attachment attachmentRequested = oManagerActiviti.getAttachment(attachmentId, taskId,
                processInstanceId);

        InputStream attachmentStream = null;
        if (attachmentRequested != null) {
            attachmentStream = taskService
                    .getAttachmentContent(attachmentRequested.getId());
        }

        if (attachmentStream == null) {
            throw new ActivitiObjectNotFoundException("Attachment for taskId '"
                    + taskId + "' doesn't have content associated with it.",
                    Attachment.class);
        }

        LOG.info("Attachment found. taskId {}, attachmentID {} With name {} ",
                taskId, attachmentId, attachmentRequested.getName());

        byte[] content = IOUtils.toByteArray(attachmentStream);

        String soSignData = BankIDUtils.checkECP(bankIDConfig.sClientId(),
                bankIDConfig.sClientSecret(), generalConfig.sHostCentral(),
                content, attachmentRequested.getName());

        return soSignData;
    }

    /**
     * Сервис для получения Attachment из execution
     *
     * @param taskId
     * @param httpResponse
     * @return
     * @throws java.io.IOException
     */
    @ApiOperation(value = "Сервис для получения Attachment из execution", notes = "#####  ObjectFileCommonController: Сервис для получения Attachment из execution #####\n\n")
    @RequestMapping(value = "/download_file_from_db_execution", method = RequestMethod.GET)
    @Transactional
    public
    @ResponseBody
    byte[] getAttachmentFromDbExecution(
            @ApiParam(value = "ИД-номер таски", required = true) @RequestParam("taskId") String taskId,
            HttpServletResponse httpResponse) throws IOException {

        // получаем по задаче ид процесса
        HistoricTaskInstance historicTaskInstanceQuery = historyService
                .createHistoricTaskInstanceQuery().taskId(taskId)
                .singleResult();
        String processInstanceId = historicTaskInstanceQuery
                .getProcessInstanceId();
        if (processInstanceId == null) {
            throw new ActivitiObjectNotFoundException(String.format(
                    "ProcessInstanceId for taskId '{%s}' not found.", taskId),
                    Attachment.class);
        }

        // получаем по ид процесса сам процесс
        HistoricProcessInstance processInstance = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).includeProcessVariables()
                .singleResult();
        if (processInstance == null) {
            throw new ActivitiObjectNotFoundException(String.format(
                    "ProcessInstance for processInstanceId '{%s}' not found.",
                    processInstanceId), Attachment.class);
        }

        // получаем коллекцию переменных процеса и прикрепленный файл
        Map<String, Object> processVariables = processInstance
                .getProcessVariables();
        BuilderAttachModelCover attachModel = null;

        if (processVariables != null) {
            List<BuilderAttachModelCover> builderAttachModelList = (List) processVariables
                    .get(FileTaskUpload.BUILDER_ATACH_MODEL_LIST);

            if (builderAttachModelList != null) {
                attachModel = builderAttachModelList.get(0);
            }
        }

        if (attachModel == null) {
            throw new ActivitiObjectNotFoundException(
                    String.format(
                            "ProcessVariable '{%s}' for processInstanceId '{%s}' not found.",
                            FileTaskUpload.BUILDER_ATACH_MODEL_LIST,
                            processInstanceId));
        }

        // Помещаем параметры контента в header
        httpResponse.setHeader("Content-disposition", "attachment; filename="
                + attachModel.getOriginalFilename());
        httpResponse.setHeader("Content-Type", attachModel.getContentType()
                + ";charset=UTF-8");
        httpResponse.setContentLength(attachModel.getByteToStringContent()
                .getBytes().length);

        return AbstractModelTask.contentStringToByte(attachModel
                .getByteToStringContent());
    }

    /**
     * Аплоад(upload) и прикрепление файла в виде атачмента к таске Activiti
     *
     * @param taskId      ИД-номер таски
     * @param description описание
     * @param file        в html это имя элемента input типа file - <input name="file" type="file" />. в HTTP заголовках - Content-Disposition: form-data; name="file" ...
     * @param nID_Subject ID авторизированого субъекта (добавляется в запрос автоматически после аутентификации пользователя)
     */
    @ApiOperation(value = "Аплоад(upload) и прикрепление файла в виде атачмента к таске Activiti", notes = "#####  ObjectFileCommonController: Аплоад(upload) и прикрепление файла в виде атачмента к таске Activiti #####\n\n"
            + "HTTP Context: http://server:port/wf/service/object/file/upload_file_as_attachment\n\n\n"
            + "Пример: http://test.igov.org.ua/wf/service/object/file/upload_file_as_attachment?taskId=68&description=ololo\n\n"
            + "\n```json\n"
            + "Ответ без ошибок:\n"
            + "{\n"
            + "  \"taskId\": \"38\",\n"
            + "  \"processInstanceId\": null,\n"
            + "  \"userId\": \"kermit\",\n"
            + "  \"name\": \"jmt.png\",\n"
            + "  \"id\": \"45\",\n"
            + "  \"type\": \"image/png;png\",\n"
            + "  \"description\": \"SomeDocumentDescription\",\n"
            + "  \"time\": 1433539278957,\n"
            + "  \"url\": null\n"
            + "}\n"
            + "\nID созданного attachment - \"id\": \"45\"\n\n"
            + "\n```\n"
            + "Ответ с ошибкой:\n\n"
            + "\n```json\n"
            + "{\"code\":\"SYSTEM_ERR\",\"message\":\"Cannot find task with id 384\"}\n"
            + "\n```\n")
    @RequestMapping(value = "/upload_file_as_attachment", method = RequestMethod.POST, produces = "application/json")
    @Transactional
    public
    @ResponseBody
    ResponseEntity putAttachmentsToExecution(
            @ApiParam(value = "ИД-номер таски", required = true) @RequestParam(value = "taskId") String taskId,
            @ApiParam(value = "в html это имя элемента input типа file - <input name=\"file\" type=\"file\" />. в HTTP заголовках - Content-Disposition: form-data; name=\"file\" ...", required = true) @RequestParam("file") MultipartFile file,
            @ApiParam(value = "описание", required = true) @RequestParam(value = "description") String description)
            throws IOException {

        ActionTaskService oManagerActiviti=new ActionTaskService();
        
        String processInstanceId = null;
        String assignee = null;

        List<Task> tasks = taskService.createTaskQuery().taskId(taskId).list();
        if (!tasks.isEmpty()) {
            Task task = tasks.iterator().next();
            processInstanceId = task.getProcessInstanceId();
            assignee = task.getAssignee() != null ? task.getAssignee()
                    : "kermit";
            LOG.debug("processInstanceId: " + processInstanceId + " taskId: "
                    + taskId + "assignee: " + assignee);
        } else {
            LOG.error("There is no tasks at all!");
        }

        identityService.setAuthenticatedUserId(assignee);

        String sFilename = file.getOriginalFilename();
        LOG.debug("sFilename=" + file.getOriginalFilename());
        sFilename = Renamer.sRenamed(sFilename);
        LOG.debug("FileExtention: " + oManagerActiviti.getFileExtention(file)
                + " fileContentType: " + file.getContentType() + "fileName: "
                + sFilename);
        LOG.debug("description: " + description);

        Attachment oAttachment = taskService.createAttachment(
                file.getContentType() + ";" + oManagerActiviti.getFileExtention(file), taskId,
                processInstanceId, sFilename,// file.getOriginalFilename()
                description, file.getInputStream());

        AttachmentCover oAttachmentCover = new AttachmentCover();
        AttachmentEntityI oAttachmentEntityI=oAttachmentCover.apply(oAttachment);
        LOG.info("(oAttachmentEntityI={})", oAttachmentEntityI.toString());
        return JsonRestUtils.toJsonResponse(oAttachmentEntityI);
        //return oAttachmentCover.apply(oAttachment);
    }

    /**
     * Аплоад(upload) и прикрепление текстового файла в виде атачмента к таске Activiti
     *
     * @param taskId       ИД-номер таски
     * @param sContentType MIME тип отправляемого файла (опциоанльно) (значение по умолчанию = "text/html")
     * @param description  описание
     * @param sFileName    имя отправляемого файла
     */
    @ApiOperation(value = "Аплоад(upload) и прикрепление текстового файла в виде атачмента к таске Activiti", notes = "#####  ObjectFileCommonController: Аплоад(upload) и прикрепление текстового файла в виде атачмента к таске Activiti #####\n\n"
            + "HTTP Context: http://server:port/wf/service/object/file/upload_content_as_attachment - Аплоад(upload) и прикрепление текстового файла в виде атачмента к таске Activiti\n\n"
            + "Пример: http://localhost:8080/wf/service/object/file/upload_content_as_attachment?nTaskId=24&sDescription=someText&sFileName=FlyWithMe.html\n\n\n"
            + "\n```json\n"
            + "Ответ без ошибок:\n"
            + "{\n"
            + "  \"taskId\": \"38\",\n"
            + "  \"processInstanceId\": null,\n"
            + "  \"userId\": \"kermit\",\n"
            + "  \"name\": \"FlyWithMe.html\",\n"
            + "  \"id\": \"25\",\n"
            + "  \"type\": \"text/html;html\",\n"
            + "  \"description\": \"someText\",\n"
            + "  \"time\": 1433539278957,\n"
            + "  \"url\": null\n"
            + "}\n\n"
            + "ID созданного attachment - \"id\": \"25\"\n"
            + "\n```\n"
            + "\nОтвет с ошибкой:\n"
            + "\n```json\n"
            + "{\"code\":\"SYSTEM_ERR\",\"message\":\"Cannot find task with id 384\"}\n"
            + "\n```\n")
    @RequestMapping(value = "/upload_content_as_attachment", method = RequestMethod.POST, produces = "application/json")
    @Transactional
    public
    @ResponseBody
    AttachmentEntityI putTextAttachmentsToExecution(
            @ApiParam(value = "Логин пользователя", required = true) @RequestParam(value = "nTaskId") String taskId,
            @ApiParam(value = "MIME тип отправляемого файла (опциоанльно) (значение по умолчанию = \"text/html\")", required = false) @RequestParam(value = "sContentType", required = false, defaultValue = "text/html") String sContentType,
            @ApiParam(value = "описание", required = true) @RequestParam(value = "sDescription") String description,
            @RequestParam(value = "sFileName") String sFileName,
            @RequestBody String sData) {

        ActionTaskService oManagerActiviti=new ActionTaskService();
        
        String processInstanceId = null;
        String assignee = null;

        List<Task> tasks = taskService.createTaskQuery().taskId(taskId).list();
        if (!tasks.isEmpty()) {
            Task task = tasks.iterator().next();
            processInstanceId = task.getProcessInstanceId();
            assignee = task.getAssignee() != null ? task.getAssignee()
                    : "kermit";
            LOG.debug("processInstanceId: " + processInstanceId + " taskId: "
                    + taskId + "assignee: " + assignee);
        } else {
            LOG.error("There is no tasks at all!");

        }

        identityService.setAuthenticatedUserId(assignee);

        String sFilename = sFileName;
        LOG.debug("sFilename=" + sFileName);
        sFilename = Renamer.sRenamed(sFilename);
        LOG.debug("FileExtention: " + oManagerActiviti.getFileExtention(sFileName)
                + " fileContentType: " + sContentType + "fileName: "
                + sFilename);
        LOG.debug("description: " + description);

        Attachment attachment = taskService.createAttachment(sContentType + ";"
                        + oManagerActiviti.getFileExtention(sFileName), taskId, processInstanceId,
                sFilename, description,
                new ByteArrayInputStream(sData.getBytes(Charsets.UTF_8)));

        AttachmentCover adapter = new AttachmentCover();

        return adapter.apply(attachment);
    }

    /**
     * @param sPathFile    полный путь к файлу, например: folder/file.html.
     * @param sContentType тип контента (опционально, по умолчанию обычный текст: text/plain)
     */
    @ApiOperation(value = "Работа с файлами-шаблонами", notes = "#####  ObjectFileCommonController: Работа с файлами-шаблонами #####\n\n"
            + "HTTP Context: https://test.region.igov.org.ua/wf/service/object/file/getPatternFile?sPathFile=full-path-file&sContentType=content-type\n\n\n"
	    + "возвращает содержимое указанного файла с указанным типом контента (если он задан).\n\n\n"
            + "Если указанный путь неверен и файл не найден -- вернется соответствующая ошибка.\n\n"
            + "Примеры:\n\n"
            + "https://test.region.igov.org.ua/wf/service/object/file/getPatternFile?sPathFile=print//subsidy_zayava.html\n\n"
            + "ответ: вернется текст исходного кода файла-шаблона\n\n"
            + "https://test.region.igov.org.ua/wf/service/object/file/getPatternFile?sPathFile=print//subsidy_zayava.html&sContentType=text/html\n\n"
            + "ответ: файл-шаблон будет отображаться в виде html-страницы")
    @RequestMapping(value = "/getPatternFile", method = RequestMethod.GET)
    public void getPatternFile(
            @ApiParam(value = "полный путь к файлу", required = true) @RequestParam(value = "sPathFile") String sPathFile,
            @ApiParam(value = "тип контента", required = false) @RequestParam(value = "sContentType", required = false) String sContentType,
            HttpServletResponse response) throws CommonServiceException {

        try {
            String contentType = sContentType == null ? Util.PATTERN_DEFAULT_CONTENT_TYPE
                    : sContentType;
            response.setContentType(contentType);
            response.setCharacterEncoding(Charsets.UTF_8.toString());
            byte[] resultObj = Util.getPatternFile(sPathFile);
            response.getOutputStream().write(resultObj);
        } catch (IllegalArgumentException | IOException e) {
            CommonServiceException newErr = new CommonServiceException(
                    "BUSINESS_ERR", e.getMessage(), e);
            newErr.setHttpStatus(HttpStatus.FORBIDDEN);
            throw newErr;
        } catch (Exception e) {
            CommonServiceException newErr = new CommonServiceException(
                    "SYSTEM_ERR", e.getMessage(), e);
            newErr.setHttpStatus(HttpStatus.FORBIDDEN);
            throw newErr;
        }
    }


    
}
