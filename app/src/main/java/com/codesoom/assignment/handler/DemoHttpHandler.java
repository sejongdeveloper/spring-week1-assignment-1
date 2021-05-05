package com.codesoom.assignment.handler;

import com.codesoom.assignment.enums.HttpMethod;
import com.codesoom.assignment.enums.HttpStatus;
import com.codesoom.assignment.exceptions.NotFoundTaskException;
import com.codesoom.assignment.models.Task;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DemoHttpHandler implements HttpHandler {

    private static Long taskIdSeq = 0L;

    private ObjectMapper objectMapper = new ObjectMapper();

    private List<Task> tasks = new ArrayList<>();

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        String method = httpExchange.getRequestMethod(); // Method
        URI uri = httpExchange.getRequestURI(); // URI
        String path = uri.getPath(); // Path
        System.out.println(method + " " + path);

        Long pathValue = null; // PathValue로 넘겨받은 값

        try {

            Task rqTask = new Task(); // Request에서 받은 데이터를 담을 객체

            // 입력값 가져오기
            InputStream inputStream = httpExchange.getRequestBody();
            String body = new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .collect(Collectors.joining("\n"));

            if(!body.isBlank()){ // Request에서 받은 데이터가 있다면 Task 객체에 넣음
                rqTask = jsonStrToTask(body);
            }

            // PathVariable이 있는지 검사한다.
            String[] splitArr = path.split("/");
            if(splitArr.length > 2){  // PathValue가 있을 경우 값 세팅 및 PathValue에 해당하는 Task의 인덱스를 구함

                // 값 세팅
                path = "/" + splitArr[1]; // API 경로
                pathValue = Long.valueOf(splitArr[2]); // PathValue(taskId)

            }

            if(method.equals(HttpMethod.GET.name()) && path.equals("/tasks")){ // Task List 조회 & 단건조회

                if(pathValue == null){
                    tasksGET(httpExchange);
                } else {
                    tasksGET(httpExchange, pathValue);
                }


            } else if(method.equals(HttpMethod.POST.name()) && path.equals("/tasks")){ // Task 등록

                tasksPOST(httpExchange, rqTask);

            } else if( method.equals(HttpMethod.PUT.name()) && path.equals("/tasks") ){ // Task 수정

                tasksPUT(httpExchange, pathValue, rqTask);

            } else if( method.equals(HttpMethod.PATCH.name()) && path.equals("/tasks") ){ // Task 수정

                tasksPATCH(httpExchange, pathValue, rqTask);

            } else if(method.equals(HttpMethod.DELETE.name()) && path.equals("/tasks")){ // Task 삭제

                tasksDELETE(httpExchange, pathValue);

            } else {

                sendResponse(httpExchange, HttpStatus.NOT_FOUND.getCodeNo(), "잘못된 요청입니다.");

            }

        } catch (NotFoundTaskException e) {

            e.printStackTrace();
            sendResponse(httpExchange, HttpStatus.NOT_FOUND.getCodeNo(), "해당하는 Task를 찾을 수 없습니다.");

        } catch (Exception e) {

            e.printStackTrace();
            sendResponse(httpExchange, HttpStatus.NOT_FOUND.getCodeNo(), "통신에 실패했습니다.");

        }

    }

    /**
     * Task List에서 ID에 해당하는 Task의 Index를 찾음
     * @param tasks
     * @param taskId
     * @return
     */
    private int findTaskIndex(List<Task> tasks, Long taskId) {

        int findTaskIndex = -1;
        for(int i = 0 ; i < tasks.size() ; i++){
            if(taskId == tasks.get(i).getId()){
                findTaskIndex = i;
            }
        }

        return findTaskIndex;
    }

    /**
     * JsonString으로 변환한다.
     * @param obj
     * @return
     * @throws IOException
     */
    private String toJsonStr(Object obj) throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        objectMapper.writeValue(outputStream, obj);

        return outputStream.toString();

    }

    /**
     * JsonString을 Task 객체로 변환한다.
     * @param content
     * @return
     * @throws JsonProcessingException
     */
    private Task jsonStrToTask(String content) throws JsonProcessingException {
        return objectMapper.readValue(content, Task.class);
    }

    /**
     * Response를 전송 메서드
     * @param httpExchange
     * @param httpStatus
     * @param resBody
     * @throws IOException
     */
    private void sendResponse(HttpExchange httpExchange, int httpStatus, String resBody) throws IOException {

        httpExchange.sendResponseHeaders(httpStatus, resBody.getBytes().length );
        System.out.println("response = " + resBody);
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(resBody.getBytes());
        outputStream.flush();
        outputStream.close();

    }

    /**
     * Task List 조회
     * @param httpExchange
     * @throws IOException
     */
    private void tasksGET(HttpExchange httpExchange) throws IOException {

        String content = null;
        int httpStatus = HttpStatus.OK.getCodeNo();

        content = toJsonStr(tasks);

        sendResponse(httpExchange, httpStatus, content);

    }

    /**
     * Task 단건 조회
     * @param httpExchange
     * @param pathValue
     * @throws IOException
     */
    private void tasksGET(HttpExchange httpExchange, Long pathValue) throws IOException {

        String content = null;
        int httpStatus = HttpStatus.OK.getCodeNo();

        int taskIndex = findTaskIndex(tasks, pathValue);// PathValue에 해당하는 Task의 인덱스를 구함

        if(pathValue == null){
            content = toJsonStr(tasks);
        } else {
            Task findTask = tasks.get(taskIndex);
            content = toJsonStr(findTask);
        }

        sendResponse(httpExchange, httpStatus, content);

    }

    /**
     * Task 등록
     * @param httpExchange
     * @param rqTask
     * @throws IOException
     */
    private void tasksPOST(HttpExchange httpExchange, Task rqTask) throws IOException {

        String content = null;
        int httpStatus = HttpStatus.CREARE.getCodeNo();

        rqTask.setId(taskIdSeq++);
        tasks.add(rqTask);

        content = toJsonStr(rqTask);

        sendResponse(httpExchange, httpStatus, content);

    }

    /**
     * Task 수정
     * @param httpExchange
     * @param pathValue
     * @param rqTask
     * @throws IOException
     */
    private void tasksPUT(HttpExchange httpExchange,  Long pathValue, Task rqTask) throws IOException {

        String content = null;
        int httpStatus = HttpStatus.OK.getCodeNo();

        int taskIndex = findTaskIndex(tasks, pathValue);// PathValue에 해당하는 Task의 인덱스를 구함

        // 해당하는 Task가 없을 경우 예외 발생
        if(taskIndex < 0){
            throw new NotFoundTaskException(pathValue);
        }

        String newTitle = rqTask.getTitle();

        Task modifiedTask = tasks.get(taskIndex);
        modifiedTask.setTitle(newTitle);

        content = toJsonStr(modifiedTask);

        sendResponse(httpExchange, httpStatus, content);

    }

    /**
     * Task 수정
     * @param httpExchange
     * @param pathValue
     * @param rqTask
     * @throws IOException
     */
    private void tasksPATCH(HttpExchange httpExchange,  Long pathValue, Task rqTask) throws IOException {

        String content = null;
        int httpStatus = HttpStatus.OK.getCodeNo();

        int taskIndex = findTaskIndex(tasks, pathValue);// PathValue에 해당하는 Task의 인덱스를 구함

        // 해당하는 Task가 없을 경우 예외 발생
        if(taskIndex < 0){
            throw new NotFoundTaskException(pathValue);
        }

        String newTitle = rqTask.getTitle();

        Task modifiedTask = tasks.get(taskIndex);
        modifiedTask.setTitle(newTitle);

        content = toJsonStr(modifiedTask);

        sendResponse(httpExchange, httpStatus, content);

    }


    /**
     * Task 삭제
     * @param httpExchange
     * @param pathValue
     * @throws IOException
     */
    private void tasksDELETE(HttpExchange httpExchange, Long pathValue) throws IOException {

        String content = "";
        int httpStatus = HttpStatus.NO_CONTENT.getCodeNo();

        int taskIndex = findTaskIndex(tasks, pathValue);// PathValue에 해당하는 Task의 인덱스를 구함

        // 해당하는 Task가 없을 경우 예외 발생
        if(taskIndex < 0){
            throw new NotFoundTaskException(pathValue);
        }

        tasks.remove(taskIndex);

        sendResponse(httpExchange, httpStatus, content);

    }


}
