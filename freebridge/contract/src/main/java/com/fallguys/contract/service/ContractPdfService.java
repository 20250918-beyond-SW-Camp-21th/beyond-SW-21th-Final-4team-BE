package com.fallguys.contract.service;

import com.fallguys.common.port.FileStorage;
import com.fallguys.contract.entity.Contract;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * PDF 생성후 S3에 저장하고 URL 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractPdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm");

    private static final String CLASSPATH_FONT = "fonts/NanumGothic.ttf";
    private static final String[] SYSTEM_FONT_PATHS = {
            // Windows fonts
            "C:\\Windows\\Fonts\\malgun.ttf",              // 맑은 고딕 (Malgun Gothic)
            "C:\\Windows\\Fonts\\gulim.ttc",               // 굴림
            "C:\\Windows\\Fonts\\batang.ttc",              // 바탕
            // Mac fonts
            "/Library/Fonts/NanumGothic.ttf",
            "/Library/Fonts/NanumBarunGothic.ttf",
            "/System/Library/Fonts/Supplemental/AppleGothic.ttf",
            "/System/Library/Fonts/AppleSDGothicNeo.ttc,0",
            // Linux fonts
            "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
            "/usr/share/fonts/truetype/nanum/NanumBarunGothic.ttf",
    };

    private static final String CONTENT_TYPE_PDF = "application/pdf";

    private final FileStorage fileStorage;

    public String generateContractPdf(Contract contract) {
        return generate(contract, false);
    }

    public String generateSignedPdf(Contract contract) {
        return generate(contract, true);
    }

    public byte[] generateContractPdfBytes(Contract contract) {
        log.info("Starting PDF generation (byte array) for contract {}", contract.getContractId());
        
        Document doc = new Document(PageSize.A4, 60, 60, 60, 60);
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();
            buildContent(doc, contract, false);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF byte array generation failed for contract {}: {}", contract.getContractId(), e.getMessage(), e);
            throw new RuntimeException("계약서 PDF 생성 실패 (Bytes)", e);
        }
    }

    private String generate(Contract contract, boolean withSignatures) {
        log.info("Starting PDF generation for contract {}", contract.getContractId());

        String suffix   = withSignatures ? "_signed" : "_contract";
        String fileName = contract.getContractId() + suffix + ".pdf";
        String key = "contracts/" + fileName;

        Document doc = new Document(PageSize.A4, 60, 60, 60, 60);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            log.info("Creating PDF document for S3 key: {}", key);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            log.info("PdfWriter created successfully");
            doc.open();
            log.info("Document opened, building content...");
            buildContent(doc, contract, withSignatures);
            log.info("Content built, closing document...");
            doc.close();
            log.info("Document closed successfully");
            fileStorage.upload(out.toByteArray(), key, CONTENT_TYPE_PDF);
        } catch (Exception e) {
            log.error("PDF generation failed for contract {}: {}", contract.getContractId(), e.getMessage(), e);
            throw new RuntimeException("계약서 PDF 생성 실패: " + fileName, e);
        }

        log.info("계약서 PDF 생성 완료: {}", key);
        return key;
    }

    public String generatePresignedUrl(String key) {
        return fileStorage.generatePresignedUrl(key);
    }

    private void buildContent(Document doc, Contract contract, boolean withSignatures)
            throws DocumentException, IOException {

        BaseFont bf = loadKoreanFont();

        Font titleFont  = new Font(bf, 18, Font.BOLD);
        Font h2Font     = new Font(bf, 12, Font.BOLD);
        Font labelFont  = new Font(bf, 10, Font.BOLD);
        Font bodyFont   = new Font(bf, 10, Font.NORMAL);
        Font smallFont  = new Font(bf,  8, Font.NORMAL, new Color(120, 120, 120));

        Paragraph title = new Paragraph("표준근로계약서", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        doc.add(title);

        Paragraph contractNo = new Paragraph("계약번호: " + safe(contract.getContractId()), smallFont);
        contractNo.setAlignment(Element.ALIGN_RIGHT);
        doc.add(contractNo);
        doc.add(Chunk.NEWLINE);

        doc.add(sectionHeader("당사자 정보", h2Font));

        PdfPTable partiesTable = new PdfPTable(2);
        partiesTable.setWidthPercentage(100);
        partiesTable.setWidths(new float[]{1f, 1f});

        PdfPCell empCell = new PdfPCell();
        empCell.setBorder(Rectangle.BOX);
        empCell.setPadding(8);
        empCell.addElement(new Paragraph("고용주 (Employer)", labelFont));
        empCell.addElement(new Paragraph("상호: " + safe(contract.getEmployerBusinessName()), bodyFont));
        empCell.addElement(new Paragraph("주소: " + safe(contract.getEmployerAddress()), bodyFont));
        empCell.addElement(new Paragraph("대표: " + safe(contract.getEmployerCEO()), bodyFont));
        partiesTable.addCell(empCell);

        PdfPCell freeCell = new PdfPCell();
        freeCell.setBorder(Rectangle.BOX);
        freeCell.setPadding(8);
        freeCell.addElement(new Paragraph("프리랜서 (Freelancer)", labelFont));
        freeCell.addElement(new Paragraph("주소: " + safe(contract.getFreelancerAddress()), bodyFont));
        freeCell.addElement(new Paragraph("연락처: " + safe(contract.getFreelancerPhone()), bodyFont));
        partiesTable.addCell(freeCell);

        doc.add(partiesTable);
        doc.add(Chunk.NEWLINE);

        doc.add(sectionHeader("계약 조건", h2Font));

        PdfPTable termsTable = labelValueTable();
        addLabelValue(termsTable, "프로젝트명", safe(contract.getProjectName()), labelFont, bodyFont);
        addLabelValue(termsTable, "계약 기간",
                formatDate(contract.getStartDate()) + " ~ " + formatDate(contract.getEndDate()),
                labelFont, bodyFont);
        addLabelValue(termsTable, "월 급여",    formatWon(contract.getBudget()), labelFont, bodyFont);
        addLabelValue(termsTable, "급여 지급일", "매월 " + safe(contract.getPaymentDay()) + "일", labelFont, bodyFont);
        addLabelValue(termsTable, "플랫폼 수수료", formatRate(contract.getCommissionRate()), labelFont, bodyFont);
        doc.add(termsTable);
        doc.add(Chunk.NEWLINE);

        doc.add(sectionHeader("근무 조건", h2Font));

        PdfPTable workTable = labelValueTable();
        addLabelValue(workTable, "근무지",   safe(contract.getWorkLocation()), labelFont, bodyFont);
        addLabelValue(workTable, "근무 시간",
                safe(contract.getWorkStartTime()) + " ~ " + safe(contract.getWorkEndTime()),
                labelFont, bodyFont);
        addLabelValue(workTable, "휴게 시간",
                safe(contract.getBreakStartTime()) + " ~ " + safe(contract.getBreakEndTime()),
                labelFont, bodyFont);
        addLabelValue(workTable, "주 근무일", safe(contract.getWorkDaysPerWeek()) + "일", labelFont, bodyFont);
        addLabelValue(workTable, "주휴일",   safe(contract.getWeeklyHoliday()), labelFont, bodyFont);
        doc.add(workTable);
        doc.add(Chunk.NEWLINE);

        doc.add(sectionHeader("업무 내용", h2Font));

        PdfPTable descTable = new PdfPTable(1);
        descTable.setWidthPercentage(100);
        PdfPCell descCell = new PdfPCell(new Paragraph(safe(contract.getJobDescription()), bodyFont));
        descCell.setBorder(Rectangle.BOX);
        descCell.setPadding(8);
        descCell.setMinimumHeight(70);
        descTable.addCell(descCell);
        doc.add(descTable);
        doc.add(Chunk.NEWLINE);

        doc.add(sectionHeader("서명", h2Font));

        PdfPTable sigTable = new PdfPTable(2);
        sigTable.setWidthPercentage(100);
        sigTable.setWidths(new float[]{1f, 1f});

        String empSignedAt  = contract.getEmployerSignedDate()  != null
                ? contract.getEmployerSignedDate().format(DT_FMT)  : null;
        String freeSignedAt = contract.getFreelancerSignedDate() != null
                ? contract.getFreelancerSignedDate().format(DT_FMT) : null;

        sigTable.addCell(signatureCell(
                "고용주", safe(contract.getEmployerCEO()), empSignedAt,
                withSignatures ? contract.getEmployerSignature() : null,
                labelFont, bodyFont));

        sigTable.addCell(signatureCell(
                "프리랜서", contract.getFreelancerName(), freeSignedAt,
                withSignatures ? contract.getFreelancerSignature() : null,
                labelFont, bodyFont));

        doc.add(sigTable);

        doc.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph("본 계약서는 FreeBridge 플랫폼을 통해 생성되었습니다.", smallFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    private PdfPCell signatureCell(String role, String name, String signedAt,
                                   String base64Sig, Font labelFont, Font bodyFont)
            throws DocumentException, IOException {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(8);
        cell.setMinimumHeight(120);

        cell.addElement(new Paragraph(role, labelFont));
        if (name != null && !name.isBlank()) {
            cell.addElement(new Paragraph("이름: " + name, bodyFont));
        }

        // 서명 이미지를 서명일 위에 배치
        if (base64Sig != null && !base64Sig.isBlank()) {
            try {
                String raw      = base64Sig.replaceFirst("^data:image/[a-z]+;base64,", "");
                byte[] imgBytes = Base64.getDecoder().decode(raw);
                Image sig = Image.getInstance(imgBytes);
                sig.scaleToFit(160, 60);
                sig.setSpacingBefore(6f);
                sig.setSpacingAfter(4f);
                cell.addElement(sig);
            } catch (Exception e) {
                log.warn("서명 이미지 임베드 실패: {}", e.getMessage());
                cell.addElement(new Paragraph("(서명 이미지를 로드할 수 없습니다)", bodyFont));
            }
        } else {
            cell.addElement(new Paragraph("\n\n(서명 미완료)", bodyFont));
        }

        if (signedAt != null) {
            cell.addElement(new Paragraph("서명일: " + signedAt, bodyFont));
        }

        return cell;
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
        } catch (Exception ignored) {}

        for (String path : SYSTEM_FONT_PATHS) {
            try {
                return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception ignored) {}
        }

        log.warn("한국어 폰트를 찾을 수 없습니다. " +
                 "src/main/resources/fonts/korean.ttf 에 NanumGothic 등을 추가하면 한글이 표시됩니다.");
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("기본 폰트 로드 실패", e);
        }
    }

    private String safe(Object val) {
        return val == null ? "-" : val.toString();
    }

    private String formatDate(java.time.LocalDate date) {
        return date == null ? "-" : date.format(DATE_FMT);
    }

    private String formatWon(Long amount) {
        return amount == null ? "-" : String.format("%,d 원", amount);
    }

    private String formatRate(Double rate) {
        return rate == null ? "-" : String.format("%.1f%%", rate * 100);
    }
}
