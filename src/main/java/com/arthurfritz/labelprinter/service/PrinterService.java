package com.arthurfritz.labelprinter.service;

import com.arthurfritz.labelprinter.dto.Example;
import com.arthurfritz.labelprinter.dto.Message;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.DocumentException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.xhtmlrenderer.pdf.ITextRenderer;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.SimpleDoc;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.Objects;

import static javax.print.DocFlavor.INPUT_STREAM.*;


@Service
public class PrinterService {

    private final String defaultPrinter;

    public PrinterService(@Value("${default.printer}") String defaultPrinter) {
        this.defaultPrinter = defaultPrinter;
    }

    public void printMessage(Message message) throws IOException, WriterException, TemplateException, DocumentException, PrintException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_29);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setDirectoryForTemplateLoading(ResourceUtils.getFile("classpath:templates"));
        Template template = cfg.getTemplate("example.ftl");
        Example example = new Example(message.getName(), message.getLastName(), createQrCodeImg(message.getQrCode()));
        StringWriter consoleWriter = new StringWriter();
        template.process(example, consoleWriter);

        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(consoleWriter.toString());
        renderer.layout();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        renderer.createPDF(outputStream);

        DocPrintJob job = selectPrinter(message.getPrinter()).createPrintJob();
        DocAttributeSet das = new HashDocAttributeSet();
        Doc document = new SimpleDoc(outputStream.toByteArray(), DocFlavor.BYTE_ARRAY.AUTOSENSE, das);
        PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
        job.print(document, pras);
    }

    private PrintService selectPrinter(String whoPrint) {
        PrintService[] printerServices = PrinterJob.lookupPrintServices();
        if (whoPrint != null) {
            for (PrintService printer : printerServices) {
                if (printer.getName().contains(whoPrint)) {
                    return printer;
                }
            }
        }
        for (PrintService printer : printerServices) {
            if (printer.getName().contains(defaultPrinter)) {
                return printer;
            }
        }
        throw new NoSuchElementException("Not found printer driver");
    }

    private String createQrCodeImg(String content) throws IOException, WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(Objects.isNull(content) ? "" : content, BarcodeFormat.QR_CODE, 300, 300);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

        String base64Img = Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());
        return "data:image/png;base64," + base64Img.replaceAll("\r|\n", "");
    }

}
