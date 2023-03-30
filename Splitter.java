import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.nio.file.Paths.get;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

public class Splitter {
    private static final int PAGE_NUM_TO_DERIVE_DIMENSIONS = 1;

    public static void main(String[] args) throws IOException {
        final Path pdfPath = get("<source pdf path>");
        final Path partDir = pdfPath.getParent().resolve("parts"); // will save to the parts sub directory in the pdf
                                                                   // directory
        if (!Files.exists(partDir)) {
            Files.createDirectory(partDir);
        }
        split(pdfPath, 5, partDir);
    }

    private static List<Path> split(Path pdfPath, int maxNumOfPagesPerSplit, Path dirToSaveSplitsTo)
            throws IOException {
        List<Path> partPaths = new ArrayList<>();
        try (InputStream pdfStream = Files.newInputStream(pdfPath, READ)) {
            PdfReader reader = new PdfReader(pdfStream);

            int totalPagesCopied = 0;
            int partNum = 0;
            do {
                int numOfPagesToCopy = totalPagesCopied + 5 <= reader.getNumberOfPages() ? maxNumOfPagesPerSplit
                        : (reader.getNumberOfPages() - totalPagesCopied);
                final Path partPath = dirToSaveSplitsTo
                        .resolve(getFormattedNamePerSplit(reader.getNumberOfPages(), maxNumOfPagesPerSplit, ++partNum));
                try (OutputStream partStream = Files.newOutputStream(partPath, CREATE_NEW, WRITE)) {
                    Document partDocument = new Document(reader.getPageSizeWithRotation(PAGE_NUM_TO_DERIVE_DIMENSIONS));
                    PdfCopy partCopy = new PdfCopy(partDocument, partStream);
                    partDocument.open();
                    do {
                        PdfImportedPage page = partCopy.getImportedPage(reader, ++totalPagesCopied);
                        partCopy.addPage(page);
                    } while (--numOfPagesToCopy > 0);
                    partDocument.close();
                    partPaths.add(partPath);
                }
            } while (totalPagesCopied < reader.getNumberOfPages());
        }
        return partPaths;
    }

    private static String getFormattedNamePerSplit(int totalPages, int pagesPerSplit, int index) {
        int numOfSplits = totalPages % pagesPerSplit == 0 ? totalPages / pagesPerSplit
                : (totalPages / pagesPerSplit) + 1;
        int numOfDigits = 0;
        int remainder = numOfSplits;
        do {
            numOfDigits++;
            remainder = remainder / 10;
        } while (remainder > 0);
        return format("%0" + numOfDigits + "d.pdf", index);
    }
}
