package com.fallguys.payment.service;

import com.fallguys.common.api.contract.ContractInfo;
import com.fallguys.common.port.FileStorage;
import com.fallguys.payment.entity.EmployerSettlement;
import com.fallguys.payment.entity.SubscriptionBilling;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentInvoicePdfService {

    private static final String CONTENT_TYPE_PDF = "application/pdf";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    private static final String CLASSPATH_FONT = "fonts/NanumGothic.ttf";
    private static final String[] SYSTEM_FONT_PATHS = {
            "C:\\Windows\\Fonts\\malgun.ttf",
            "C:\\Windows\\Fonts\\gulim.ttc",
            "C:\\Windows\\Fonts\\batang.ttc",
            "/Library/Fonts/NanumGothic.ttf",
            "/Library/Fonts/NanumBarunGothic.ttf",
            "/System/Library/Fonts/Supplemental/AppleGothic.ttf",
            "/System/Library/Fonts/AppleSDGothicNeo.ttc,0",
            "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
            "/usr/share/fonts/truetype/nanum/NanumBarunGothic.ttf",
    };

    private final FileStorage fileStorage;

    public String generateServiceFeeInvoice(EmployerSettlement settlement, ContractInfo contract) {
        String fileName = "service-fee-contract-" + settlement.getContractId()
                + "-installment-" + settlement.getInstallmentNumber() + ".pdf";
        String key = "service-fee/" + fileName;

        byte[] pdfBytes = buildServiceFeePdf(settlement, contract);
        fileStorage.upload(pdfBytes, key, CONTENT_TYPE_PDF);
        return key;
    }

    public String generateSubscriptionInvoice(SubscriptionBilling billing) {
        String fileName = "subscription-billing-" + billing.getId() + ".pdf";
        String key = "subscription-fee/" + fileName;

        byte[] pdfBytes = buildSubscriptionPdf(billing);
        fileStorage.upload(pdfBytes, key, CONTENT_TYPE_PDF);
        return key;
    }

    public String generatePresignedUrl(String key) {
        return fileStorage.generatePresignedUrl(key);
    }

    private byte[] buildServiceFeePdf(EmployerSettlement settlement, ContractInfo contract) {
        Document doc = new Document(PageSize.A4, 60, 60, 60, 60);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();

            BaseFont bf = loadKoreanFont();
            Font titleFont = new Font(bf, 18, Font.BOLD);
            Font h2Font = new Font(bf, 12, Font.BOLD);
            Font labelFont = new Font(bf, 10, Font.BOLD);
            Font bodyFont = new Font(bf, 10, Font.NORMAL);
            Font smallFont = new Font(bf, 8, Font.NORMAL, new Color(120, 120, 120));

            Paragraph title = new Paragraph("서비스 수수료 인보이스", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(6);
            doc.add(title);

            Paragraph invoiceNo = new Paragraph(
                    "인보이스 번호: SVC-" + settlement.getContractId() + "-" + settlement.getInstallmentNumber(),
                    smallFont);
            invoiceNo.setAlignment(Element.ALIGN_RIGHT);
            doc.add(invoiceNo);
            doc.add(Chunk.NEWLINE);

            doc.add(sectionHeader("기본 정보", h2Font));
            PdfPTable infoTable = labelValueTable();
            addLabelValue(infoTable, "발행일", formatDate(LocalDate.now()), labelFont, bodyFont);
            addLabelValue(infoTable, "고용주", safe(contract.employerBusinessName()), labelFont, bodyFont);
            addLabelValue(infoTable, "프로젝트명", safe(contract.projectName()), labelFont, bodyFont);
            addLabelValue(infoTable, "회차", safe(settlement.getInstallmentNumber()), labelFont, bodyFont);
            doc.add(infoTable);
            doc.add(Chunk.NEWLINE);

            doc.add(sectionHeader("청구 내역", h2Font));
            PdfPTable feeTable = labelValueTable();
            addLabelValue(feeTable, "서비스 수수료", formatWon(settlement.getPlatformFee()), labelFont, bodyFont);
            addLabelValue(feeTable, "결제 총액(참고)", formatWon(settlement.getTotalPayment()), labelFont, bodyFont);
            doc.add(feeTable);

            doc.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph("본 인보이스는 FreeBridge 플랫폼에서 자동 생성되었습니다.", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("서비스 수수료 인보이스 PDF 생성 실패: contractId={}, installment={}, error={}",
                    settlement.getContractId(), settlement.getInstallmentNumber(), e.getMessage());
            throw new RuntimeException("서비스 수수료 인보이스 PDF 생성 실패", e);
        }
    }

    private byte[] buildSubscriptionPdf(SubscriptionBilling billing) {
        Document doc = new Document(PageSize.A4, 60, 60, 60, 60);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();

            BaseFont bf = loadKoreanFont();
            Font titleFont = new Font(bf, 18, Font.BOLD);
            Font h2Font = new Font(bf, 12, Font.BOLD);
            Font labelFont = new Font(bf, 10, Font.BOLD);
            Font bodyFont = new Font(bf, 10, Font.NORMAL);
            Font smallFont = new Font(bf, 8, Font.NORMAL, new Color(120, 120, 120));

            Paragraph title = new Paragraph("구독 결제 인보이스", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(6);
            doc.add(title);

            Paragraph invoiceNo = new Paragraph("인보이스 번호: SUB-" + billing.getId(), smallFont);
            invoiceNo.setAlignment(Element.ALIGN_RIGHT);
            doc.add(invoiceNo);
            doc.add(Chunk.NEWLINE);

            doc.add(sectionHeader("기본 정보", h2Font));
            PdfPTable infoTable = labelValueTable();
            addLabelValue(infoTable, "발행일", formatDate(LocalDate.now()), labelFont, bodyFont);
            addLabelValue(infoTable, "고용주 ID", safe(billing.getEmployerId()), labelFont, bodyFont);
            addLabelValue(infoTable, "플랜", safe(billing.getPlanType()), labelFont, bodyFont);
            addLabelValue(infoTable, "청구일", formatDate(billing.getBillingDate()), labelFont, bodyFont);
            addLabelValue(infoTable, "결제일", formatDate(billing.getPaidDate()), labelFont, bodyFont);
            doc.add(infoTable);
            doc.add(Chunk.NEWLINE);

            doc.add(sectionHeader("청구 내역", h2Font));
            PdfPTable feeTable = labelValueTable();
            addLabelValue(feeTable, "구독 요금", formatWon(billing.getAmount()), labelFont, bodyFont);
            doc.add(feeTable);

            doc.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph("본 인보이스는 FreeBridge 플랫폼에서 자동 생성되었습니다.", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("구독 결제 인보이스 PDF 생성 실패: billingId={}, error={}", billing.getId(), e.getMessage());
            throw new RuntimeException("구독 결제 인보이스 PDF 생성 실패", e);
        }
    }

    private Paragraph sectionHeader(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(2);
        p.setSpacingAfter(4);
        return p;
    }

    private PdfPTable labelValueTable() throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 2.8f});
        return table;
    }

    private void addLabelValue(PdfPTable table, String label, String value,
                               Font labelFont, Font bodyFont) {
        PdfPCell lc = new PdfPCell(new Paragraph(label, labelFont));
        lc.setBorder(Rectangle.BOX);
        lc.setPadding(6);
        lc.setBackgroundColor(new Color(245, 245, 245));
        table.addCell(lc);

        PdfPCell vc = new PdfPCell(new Paragraph(value, bodyFont));
        vc.setBorder(Rectangle.BOX);
        vc.setPadding(6);
        table.addCell(vc);
    }

    private BaseFont loadKoreanFont() {
        try {
            var stream = getClass().getClassLoader().getResourceAsStream(CLASSPATH_FONT);
            if (stream != null) {
                byte[] bytes = stream.readAllBytes();
                return BaseFont.createFont(CLASSPATH_FONT, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
            }
        } catch (Exception ignored) { }

        for (String path : SYSTEM_FONT_PATHS) {
            try {
                return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception ignored) { }
        }

        log.warn("한국어 폰트를 찾을 수 없습니다. 기본 폰트를 사용합니다.");
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("기본 폰트 로드 실패", e);
        }
    }

    private String safe(Object val) {
        return val == null ? "-" : val.toString();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FMT);
    }

    private String formatWon(Long amount) {
        return amount == null ? "-" : String.format("%,d 원", amount);
    }
}
