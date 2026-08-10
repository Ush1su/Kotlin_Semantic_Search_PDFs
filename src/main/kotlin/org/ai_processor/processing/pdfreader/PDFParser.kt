package org.ai_processor.processing.pdfreader

import org.ai_processor.processing.pdfreader.parsers.BlockParser
import org.ai_processor.processing.pdfreader.parsers.CaptionBlockParser
import org.ai_processor.processing.pdfreader.parsers.HeadingBlockParser
import org.ai_processor.processing.pdfreader.parsers.ListBlockParser
import org.ai_processor.processing.pdfreader.parsers.ParagraphBlockParser
import org.ai_processor.processing.pdfreader.parsers.TableBlockParser
import org.opendataloader.pdf.api.Config
import org.opendataloader.pdf.api.OpenDataLoaderPDF
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

@Service
class PDFParser(
    private val jsonMapper: JsonMapper
) {

    private val tempRoot = Path.of(DEFAULT_TEMP_DIRECTORY)

    /** The block types the reader supports, keyed by OpenDataLoader node type. */
    private val blockParsers: Map<String, BlockParser> = listOf(
        ParagraphBlockParser,
        HeadingBlockParser,
        CaptionBlockParser,
        ListBlockParser,
        TableBlockParser
    ).associateBy { it.nodeType }

    fun parse(
        filepath: Path,
        documentId: UUID
    ): ParsedPDF {
        Files.createDirectories(tempRoot)

        val tempDirectory = Files.createTempDirectory(
            tempRoot,
            "$documentId-"
        )

        try {
            parseWithOpenDataLoader(
                filepath = filepath,
                outputDirectory = tempDirectory
            )

            val jsonPath = getJsonPath(
                filepath = filepath,
                outputDirectory = tempDirectory
            )

            val root = jsonMapper.readTree(jsonPath.toFile())

            return buildParsedPdf(
                root = root,
                documentId = documentId
            )
        } catch (exception: Exception) {
            throw PdfParsingException(
                message = "Failed to parse PDF: $filepath",
                cause = exception
            )
        } finally {
            deleteDirectory(tempDirectory)
        }
    }

    private fun parseWithOpenDataLoader(
        filepath: Path,
        outputDirectory: Path
    ) {
        val config = Config().apply {
            setOutputFolder(outputDirectory.toString())
            setGenerateJSON(true)
            setGenerateMarkdown(false)
            setGenerateHtml(false)
            setGeneratePDF(false)
        }

        OpenDataLoaderPDF.processFile(
            filepath.toAbsolutePath().toString(),
            config
        )
    }

    private fun buildParsedPdf(
        root: JsonNode,
        documentId: UUID
    ): ParsedPDF {
        val blocks = root[OdlJson.KIDS]
            ?.asSequence()
            ?.flatMap { collectBlockNodes(it) }
            ?.mapIndexed { index, node -> parseBlock(node, index) }
            ?.toList()
            ?: emptyList()

        return ParsedPDF(
            documentId = documentId,
            numberOfPages = root[OdlJson.NUMBER_OF_PAGES].asInt(),
            title = root[OdlJson.TITLE]?.takeUnless { it.isNull }?.asString(),
            author = root[OdlJson.AUTHOR]?.takeUnless { it.isNull }?.asString(),
            blocks = blocks
        )
    }

    private fun parseBlock(
        node: JsonNode,
        blockIndex: Int
    ): ParsedBlock {
        val nodeType = node[OdlJson.TYPE]?.asString()

        val parser = blockParsers[nodeType]
            ?: throw PdfParsingException("Unsupported block type: $nodeType")

        return parser.parse(node, blockIndex)
    }

    /**
     * Walks the tree and keeps the nodes a block parser is registered for.
     * A text block only groups other nodes, so it contributes its kids rather
     * than itself. Anything else is not part of the extracted document.
     */
    private fun collectBlockNodes(
        node: JsonNode
    ): Sequence<JsonNode> {
        val nodeType = node[OdlJson.TYPE]?.asString()

        return when {
            nodeType in blockParsers -> sequenceOf(node)

            nodeType == OdlJson.TEXT_BLOCK ->
                node[OdlJson.KIDS]
                    ?.asSequence()
                    ?.flatMap { collectBlockNodes(it) }
                    ?: emptySequence()

            else -> emptySequence()
        }
    }

    private fun getJsonPath(
        filepath: Path,
        outputDirectory: Path
    ): Path {
        val filename = filepath.fileName.toString()

        val filenameWithoutExtension =
            filename.substringBeforeLast('.')

        return outputDirectory.resolve(
            "$filenameWithoutExtension.json"
        )
    }

    private fun deleteDirectory(directory: Path) {
        if (!Files.exists(directory)) {
            return
        }

        Files.walk(directory).use { paths ->
            paths
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        }
    }

    companion object {
        const val DEFAULT_TEMP_DIRECTORY = "temp"
    }
}
