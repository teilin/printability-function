package io.devgeek.functions;

import com.google.gson.Gson;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import no.digipost.print.validate.PdfValidationError;
import no.digipost.print.validate.PdfValidationResult;
import no.digipost.print.validate.PdfValidationSettings;
import no.digipost.print.validate.PdfValidator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    @FunctionName("DigipostPrintable")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<byte[]>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        try {
        // Parse query parameter
        final byte[] body = request.getBody().get();
        
        //Map<String, String> queryParameters = request.getQueryParameters();
        //final String fileName = queryParameters.get("filename");

        final Gson gson = new Gson();
        PrintableResponse response = new PrintableResponse();

        /*if(!fileName.contains(".pdf")) {
            response.errors.add("Unsupported file type.");
            response.errors.add("Only pdf files is supported");
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(gson.toJson(response))
                .build();
        }*/

        PdfValidator pdfValidator = new PdfValidator();
        PdfValidationSettings printValidationSettings = new PdfValidationSettings(true, true, true, true);
        PdfValidationResult pdfValidationResult = pdfValidator.validate(body, printValidationSettings);

        response.okForPrint = pdfValidationResult.okForPrint;
        response.okForWeb = pdfValidationResult.okForWeb;
        response.pages = pdfValidationResult.pages;
        
        if(pdfValidationResult.errors != null && pdfValidationResult.errors.size() > 0) {
            for (PdfValidationError error : pdfValidationResult.errors) {
                String strError = pdfValidationResult.formattedValidationErrorMessage(error);
                response.errors.add(strError);
            }
        }
        String json = gson.toJson(response);

        return request.createResponseBuilder(HttpStatus.OK)
                .body(json)
                .build();
        } catch(Exception e) {
            final Gson gson = new Gson();
            PrintableResponse response = new PrintableResponse();
            response.errors.add("Something went wrong");
            response.errors.add(e.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            response.errors.add(sw.toString());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(gson.toJson(response))
                .build();
        }
    }

    public class PrintableResponse {
        Boolean okForPrint = false;
        Boolean okForWeb = false;
        Integer pages = 0;
        ArrayList<String> errors = new ArrayList<String>();
    }
}
