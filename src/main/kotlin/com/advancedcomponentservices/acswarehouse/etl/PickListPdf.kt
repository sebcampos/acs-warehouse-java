package com.advancedcomponentservices.acswarehouse.etl
import com.advancedcomponentservices.acswarehouse.db.models.Order
import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.Phrase
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.ColumnText
import com.lowagie.text.pdf.PdfContentByte
import com.lowagie.text.pdf.PdfCopy
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfPageEventHelper
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfTemplate
import com.lowagie.text.pdf.PdfWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter

fun generatePickList(outPathDir: String, orders: List<Order>) {
    val maxOrderNum = orders.maxOf { it.num }
    val minOrderNum = orders.minOf { it.num }
    val grouped = orders.groupBy { it.num }
    val document = Document()
    val outputStream = FileOutputStream("$outPathDir$minOrderNum-$maxOrderNum.pdf")
    val pdfCopy = PdfCopy(document, outputStream)
    document.open()

    for ((num, rows) in grouped) {
        val singlePdf: ByteArrayOutputStream = processOrder(num.toString(), rows)
        val reader = PdfReader(ByteArrayInputStream(singlePdf.toByteArray()))
        val totalPages = reader.numberOfPages

        for (i in 1..totalPages) {
            val page = pdfCopy.getImportedPage(reader, i)
            pdfCopy.addPage(page)
        }

        reader.close()
    }

    document.close()
    outputStream.close()
}

private fun processOrder(orderNumber: String, orders: List<Order>): ByteArrayOutputStream {
    val document = Document(PageSize.LETTER, 10f, 10f, 15f, 15f)
    val outputStream = ByteArrayOutputStream()
    val writer = PdfWriter.getInstance(document, outputStream)
    writer.pageEvent = PageNumberEvent(orderNumber)
    document.open()

    val boldFont = Font(Font.HELVETICA, 12f, Font.BOLD)
    val normalFont = Font(Font.HELVETICA, 10f, Font.NORMAL)


    document.add(Paragraph("Packing Slip $orderNumber", boldFont).apply {
        alignment = Element.ALIGN_CENTER
        spacingAfter = 10f
    })

    val row = orders.first()
    val name = row.name
    val po = row.po
    var shipToAddress = row.shipToAddress
    val shipToAddress2 = row.shipToAddress2
    val orderNum = row.num
    val date = row.date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
    val shipDate = row.shipDate?.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) ?: "-"

    if (!shipToAddress2.isNullOrBlank()) {
        shipToAddress = "$shipToAddress\n$shipToAddress2"
    }

    // Create a parent table with 2 columns: Ship To | Order Info
    val outerTable = PdfPTable(2)
    outerTable.widthPercentage = 100f
    outerTable.setWidths(floatArrayOf(1.5f, 1f)) // Adjust width ratio as needed

    // ------------------------
    // LEFT: Ship To Table
    // ------------------------
    val shipToTable = PdfPTable(1)
    shipToTable.defaultCell.border = Rectangle.NO_BORDER

    shipToTable.addCell(Phrase("$name PO $po", normalFont))

    val shipToInnerTable = PdfPTable(2)
    shipToInnerTable.setWidths(floatArrayOf(1f, 3f))
    shipToInnerTable.widthPercentage = 100f

    // Add "Ship To:" and address cells
    val labelCell = PdfPCell(Phrase("Ship To:", normalFont))
    labelCell.border = Rectangle.NO_BORDER

    val addressCell = PdfPCell(Phrase(shipToAddress, normalFont))
    addressCell.border = Rectangle.NO_BORDER

    shipToInnerTable.addCell(labelCell)
    shipToInnerTable.addCell(addressCell)

    // Put the inner table into a cell of the outer table
    val nestedCell = PdfPCell(shipToInnerTable)
    nestedCell.border = Rectangle.NO_BORDER
    shipToTable.addCell(nestedCell)

    val leftCell = PdfPCell(shipToTable)
    leftCell.border = Rectangle.NO_BORDER
    outerTable.addCell(leftCell)

    // ------------------------
    // RIGHT: Order Info Table
    // ------------------------
    val orderInfoTable = PdfPTable(2)
    orderInfoTable.defaultCell.border = Rectangle.NO_BORDER
    orderInfoTable.setWidths(floatArrayOf(1f, 1.5f))
    orderInfoTable.widthPercentage = 100f

    fun orderRow(label: String, value: String, isBold: Boolean = false) {
        orderInfoTable.addCell(Phrase(label, normalFont))
        orderInfoTable.addCell(Phrase(value, if (isBold) boldFont else normalFont))
    }

    orderRow("Order #", orderNum.toString(), true)
    orderRow("Date", date)
    orderRow("User", "-")
    orderRow("Ship Date", shipDate)

    val rightCell = PdfPCell(orderInfoTable)
    rightCell.border = Rectangle.NO_BORDER
    outerTable.addCell(rightCell)

    // ------------------------
    // Add to Document
    // ------------------------
    document.add(outerTable)
    document.add(Paragraph(" ")) // Spacer after the block



    // table header
    val colWidths = floatArrayOf(12f, 65f, 18f, 22f, 22f, 22f)
    val table = PdfPTable(colWidths)
    table.widthPercentage = 100f
    val headers = listOf("Item", "Description", "Kit", "Bin Location", "BP-Location", "Order Qty")
    headers.forEach { header ->
        val cell = PdfPCell(Phrase(header))
        cell.border = Rectangle.BOX
        table.addCell(cell)
    }

    // table body
    for (order in orders) {
        val item = order.baseSku ?: row.item
        val onHand = row.onHand
        val description = "${order.itemDescription}\nONQ: $onHand"
        val kit = if (order.isKit) "Yes" else " "
        val binLocation = order.binLocation?.trim() ?: ""
        val bpLocation = order.bulkBinLocation?.trim() ?: ""
        val qty = order.backOrdered.toString()

        listOf(
            item,
            description,
            kit,
            binLocation,
            bpLocation,
            qty
        ).forEachIndexed {i, content ->
            val font = if (i==5) boldFont else normalFont
            val cell = PdfPCell(Phrase(content, font))
            cell.border = Rectangle.BOX
            table.addCell(cell)
        }
    }

    document.add(Paragraph("\n"))
    document.add(table)
    document.close()
    writer.close()
    return outputStream

}

private class PageNumberEvent(val orderNum: String) : PdfPageEventHelper() {
    private lateinit var totalTemplate: PdfTemplate
    private lateinit var baseFont: BaseFont

    override fun onOpenDocument(writer: PdfWriter, document: Document) {
        totalTemplate = writer.directContent.createTemplate(30f, 16f)
        baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED)
    }

    override fun onEndPage(writer: PdfWriter, document: Document) {
        val cb = writer.directContent
        val pageNumber = "$orderNum Page ${writer.pageNumber} of "

        val x = (document.pageSize.right + document.pageSize.left) / 2
        val y = document.pageSize.bottom + 15f

        cb.beginText()
        cb.setFontAndSize(baseFont, 9f)
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, pageNumber, x, y, 0f)
        cb.endText()

        // Add the template (the "Y" part)
        cb.addTemplate(totalTemplate, x + 30f, y)
    }

    override fun onCloseDocument(writer: PdfWriter, document: Document) {
        totalTemplate.beginText()
        totalTemplate.setFontAndSize(baseFont, 9f)
        totalTemplate.showText("${writer.pageNumber - 1}")
        totalTemplate.endText()
    }
}