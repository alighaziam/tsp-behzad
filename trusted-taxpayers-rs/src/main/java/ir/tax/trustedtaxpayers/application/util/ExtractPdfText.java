package ir.tax.trustedtaxpayers.application.util;

import java.io.IOException;
// NOTE: PDFBox dependency is required for this utility to work
// Add to pom.xml: <dependency>
//     <groupId>org.apache.pdfbox</groupId>
//     <artifactId>pdfbox</artifactId>
//     <version>2.0.29</version>
// </dependency>
// import org.apache.pdfbox.Loader;
// import org.apache.pdfbox.pdmodel.PDDocument;
// import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility entrypoint for extracting Persian PDF resources to UTF-8 text files.
 * 
 * NOTE: This utility requires Apache PDFBox dependency to be added to pom.xml.
 * Currently commented out due to missing dependency.
 */
public final class ExtractPdfText {
    private static final Logger logger = LoggerFactory.getLogger(ExtractPdfText.class);

    public static void main(String[] args) throws IOException {
        logger.warn("ExtractPdfText utility is disabled. Please add PDFBox dependency to pom.xml to enable.");
        // NOTE: PDFBox code commented out until dependency is added
        /*
        Path workspace = args.length > 0 ? Path.of(args[0]) : Path.of("").toAbsolutePath();
        Map<Path, Path> targets = new LinkedHashMap<>();
        targets.put(
            Path.of("trusted-taxpayers-rs", "src", "main", "resources", "document", "RC_IITP_IS_V7_6.pdf"),
            workspace.resolve("RC_IITP_IS_V7_6.txt")
        );
        targets.put(
            Path.of("trusted-taxpayers-rs", "src", "main", "resources", "document", "جدول شماره1.pdf"),
            workspace.resolve("table1.txt")
        );
        targets.put(
            Path.of(
                "trusted-taxpayers-rs",
                "src",
                "main",
                "resources",
                "document",
                "راهنمای اتصال به زیرسامانه جمع آوری SDK بدون گواهی-جاوا_8.pdf"
            ),
            workspace.resolve("sdk-guide.txt")
        );

        for (Map.Entry<Path, Path> entry : targets.entrySet()) {
            Path pdfPath = workspace.resolve(entry.getKey()).normalize();
            Path txtPath = entry.getValue().normalize();

            try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String text = stripper.getText(document);
                Files.writeString(txtPath, text, StandardCharsets.UTF_8);
                logger.info("Extracted {} → {}", pdfPath, txtPath);
            }
        }
        */
    }
}

