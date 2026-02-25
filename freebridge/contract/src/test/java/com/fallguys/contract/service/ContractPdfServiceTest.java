package com.fallguys.contract.service;

import com.fallguys.contract.entity.Contract;
import com.fallguys.contract.entity.ContractStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ContractPdfService 통합 테스트
 * 실제 PDF 파일을 생성하여 검증합니다.
 */
class ContractPdfServiceTest {

    private ContractPdfService contractPdfService;
    private Contract testContract;
    private String testPdfDir;

    @BeforeEach
    void setUp() {
        contractPdfService = new ContractPdfService();

        // 테스트용 임시 디렉토리 설정
        testPdfDir = "./test-pdfs/contracts";
        ReflectionTestUtils.setField(contractPdfService, "pdfDir", testPdfDir);
        ReflectionTestUtils.setField(contractPdfService, "urlPrefix", "/test-pdfs/contracts");

        // 테스트용 계약 데이터
        testContract = new Contract();
        testContract.setId(1L);
        testContract.setContractId(1001L);
        testContract.setProjectName("테스트 프로젝트");
        testContract.setFreelancerId(100L);
        testContract.setEmployerId(200L);
        testContract.setStartDate(LocalDate.of(2024, 1, 1));
        testContract.setEndDate(LocalDate.of(2024, 12, 31));
        testContract.setBudget(5000000L);
        testContract.setCommissionRate(0.05);
        testContract.setPaymentDay(25);
        testContract.setStatus(ContractStatus.WAITING_SIGNATURE);

        testContract.setJobDescription("웹 개발 프로젝트");
        testContract.setWorkLocation("원격근무");
        testContract.setWorkStartTime("09:00");
        testContract.setWorkEndTime("18:00");
        testContract.setBreakStartTime("12:00");
        testContract.setBreakEndTime("13:00");
        testContract.setWorkDaysPerWeek(5);
        testContract.setWeeklyHoliday("토, 일");

        testContract.setEmployerBusinessName("테스트회사");
        testContract.setEmployerAddress("서울시 강남구");
        testContract.setEmployerCEO("김대표");
        testContract.setFreelancerAddress("서울시 마포구");
        testContract.setFreelancerPhone("010-1234-5678");

        testContract.setEmployerSignature("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
        testContract.setEmployerSignedDate(LocalDateTime.now());
    }

    @AfterEach
    void tearDown() throws IOException {
        // 테스트 후 생성된 PDF 파일 및 디렉토리 삭제
        Path testPath = Path.of(testPdfDir);
        if (Files.exists(testPath)) {
            Files.walk(testPath)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                        }
                    });
        }
    }

    @Test
    @DisplayName("generateContractPdf()는 서명되지 않은 계약서 PDF를 생성한다")
    void generateContractPdf_createsUnsignedPdf() {
        String pdfUrl = contractPdfService.generateContractPdf(testContract);

        assertNotNull(pdfUrl);
        assertTrue(pdfUrl.contains("1001_contract.pdf"));

        // 파일이 실제로 생성되었는지 확인
        File pdfFile = new File(testPdfDir + "/1001_contract.pdf");
        assertTrue(pdfFile.exists());
        assertTrue(pdfFile.length() > 0, "PDF 파일이 비어있지 않아야 합니다");
    }

    @Test
    @DisplayName("generateSignedPdf()는 서명된 계약서 PDF를 생성한다")
    void generateSignedPdf_createsSignedPdf() {
        testContract.setFreelancerSignature("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
        testContract.setFreelancerSignedDate(LocalDateTime.now());

        String pdfUrl = contractPdfService.generateSignedPdf(testContract);

        assertNotNull(pdfUrl);
        assertTrue(pdfUrl.contains("1001_signed.pdf"));

        // 파일이 실제로 생성되었는지 확인
        File pdfFile = new File(testPdfDir + "/1001_signed.pdf");
        assertTrue(pdfFile.exists());
        assertTrue(pdfFile.length() > 0, "PDF 파일이 비어있지 않아야 합니다");
    }

    @Test
    @DisplayName("PDF 생성 시 디렉토리가 자동으로 생성된다")
    void createsPdfDirectoryIfNotExists() throws IOException {
        // 디렉토리가 없는 상태에서 시작
        Path testPath = Path.of(testPdfDir);
        if (Files.exists(testPath)) {
            Files.walk(testPath)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                        }
                    });
        }

        assertFalse(Files.exists(testPath));

        contractPdfService.generateContractPdf(testContract);

        assertTrue(Files.exists(testPath), "PDF 디렉토리가 자동으로 생성되어야 합니다");
    }

    @Test
    @DisplayName("서명되지 않은 PDF와 서명된 PDF는 다른 파일명을 가진다")
    void unsignedAndSignedPdfsHaveDifferentNames() {
        String unsignedPdfUrl = contractPdfService.generateContractPdf(testContract);

        testContract.setFreelancerSignature("data:image/png;base64,abc");
        testContract.setFreelancerSignedDate(LocalDateTime.now());
        String signedPdfUrl = contractPdfService.generateSignedPdf(testContract);

        assertNotEquals(unsignedPdfUrl, signedPdfUrl);
        assertTrue(unsignedPdfUrl.contains("_contract.pdf"));
        assertTrue(signedPdfUrl.contains("_signed.pdf"));

        // 두 파일 모두 존재하는지 확인
        File unsignedFile = new File(testPdfDir + "/1001_contract.pdf");
        File signedFile = new File(testPdfDir + "/1001_signed.pdf");
        assertTrue(unsignedFile.exists());
        assertTrue(signedFile.exists());
    }

    @Test
    @DisplayName("null 값이 있는 계약도 PDF 생성이 가능하다")
    void handleContractWithNullValues() {
        Contract minimalContract = new Contract();
        minimalContract.setId(2L);
        minimalContract.setContractId(1002L);
        minimalContract.setProjectName("최소 계약");
        minimalContract.setStatus(ContractStatus.WAITING_SIGNATURE);

        assertDoesNotThrow(() -> {
            String pdfUrl = contractPdfService.generateContractPdf(minimalContract);
            assertNotNull(pdfUrl);

            File pdfFile = new File(testPdfDir + "/1002_contract.pdf");
            assertTrue(pdfFile.exists());
        });
    }

    @Test
    @DisplayName("같은 계약 ID로 여러 번 PDF 생성 시 파일이 덮어씌워진다")
    void regeneratingPdfOverwritesExistingFile() {
        // 첫 번째 PDF 생성
        String firstPdfUrl = contractPdfService.generateContractPdf(testContract);
        File pdfFile = new File(testPdfDir + "/1001_contract.pdf");
        long firstFileSize = pdfFile.length();

        // 계약 내용 변경 후 다시 생성
        testContract.setProjectName("변경된 프로젝트명");
        String secondPdfUrl = contractPdfService.generateContractPdf(testContract);

        assertEquals(firstPdfUrl, secondPdfUrl, "URL은 동일해야 합니다");
        assertTrue(pdfFile.exists(), "파일이 존재해야 합니다");
        // 파일 크기가 변경되었을 수 있음 (내용이 달라졌으므로)
        assertTrue(pdfFile.length() > 0);
    }

    @Test
    @DisplayName("잘못된 Base64 서명 이미지가 있어도 PDF 생성이 실패하지 않는다")
    void handlesInvalidSignatureImage() {
        testContract.setEmployerSignature("invalid-base64-data");

        assertDoesNotThrow(() -> {
            String pdfUrl = contractPdfService.generateContractPdf(testContract);
            assertNotNull(pdfUrl);

            File pdfFile = new File(testPdfDir + "/1001_contract.pdf");
            assertTrue(pdfFile.exists());
        });
    }

    @Test
    @DisplayName("양측 서명이 모두 있을 때 서명된 PDF에 포함된다")
    void signedPdfIncludesBothSignatures() {
        testContract.setEmployerSignature("data:image/png;base64,emp");
        testContract.setEmployerSignedDate(LocalDateTime.now());
        testContract.setFreelancerSignature("data:image/png;base64,free");
        testContract.setFreelancerSignedDate(LocalDateTime.now());

        String pdfUrl = contractPdfService.generateSignedPdf(testContract);

        assertNotNull(pdfUrl);
        File pdfFile = new File(testPdfDir + "/1001_signed.pdf");
        assertTrue(pdfFile.exists());
        assertTrue(pdfFile.length() > 0);
    }

    @Test
    @DisplayName("계약 ID가 다르면 다른 파일명으로 PDF가 생성된다")
    void differentContractIdsCreateDifferentFiles() {
        Contract contract1 = new Contract();
        contract1.setId(1L);
        contract1.setContractId(1001L);
        contract1.setProjectName("계약1");
        contract1.setStatus(ContractStatus.WAITING_SIGNATURE);

        Contract contract2 = new Contract();
        contract2.setId(2L);
        contract2.setContractId(1002L);
        contract2.setProjectName("계약2");
        contract2.setStatus(ContractStatus.WAITING_SIGNATURE);

        String pdf1Url = contractPdfService.generateContractPdf(contract1);
        String pdf2Url = contractPdfService.generateContractPdf(contract2);

        assertNotEquals(pdf1Url, pdf2Url);
        assertTrue(new File(testPdfDir + "/1001_contract.pdf").exists());
        assertTrue(new File(testPdfDir + "/1002_contract.pdf").exists());
    }
}