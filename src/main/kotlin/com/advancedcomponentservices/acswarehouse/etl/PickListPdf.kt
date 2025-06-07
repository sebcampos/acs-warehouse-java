package com.advancedcomponentservices.acswarehouse.etl
import com.advancedcomponentservices.acswarehouse.db.models.Order
import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.Phrase
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.PdfCopy
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

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
    document.open()

    val boldFont = Font(Font.HELVETICA, 12f, Font.BOLD)
    val normalFont = Font(Font.HELVETICA, 10f, Font.NORMAL)


    document.add(Paragraph("Packing Slip $orderNumber").apply {
        alignment = Element.ALIGN_CENTER
        spacingAfter = 10f
    })

    val row = orders.first()

    // header
    document.add(Paragraph("${row.name} PO ${row.po}", normalFont))
    document.add(Paragraph("Ship To: ", normalFont))
    document.add(Paragraph(row.shipToAddress, normalFont))
    row.shipToAddress2?.let {
        document.add(Paragraph(it, normalFont))
    }
    document.add(Paragraph("${row.shipToCity}, ${row.shipToState} ${row.shipToZip}", normalFont))


    // order info in the top right
    val orderInfo = PdfPTable(2)
    orderInfo.horizontalAlignment = Element.ALIGN_RIGHT
    orderInfo.addCell(Phrase("Order #", normalFont))
    orderInfo.addCell(Phrase(row.num.toString(), boldFont))
    orderInfo.addCell(Phrase("Date", normalFont))
    orderInfo.addCell(Phrase(row.date.toString(), normalFont))
    orderInfo.addCell(Phrase("User", normalFont))
    orderInfo.addCell(Phrase("-", normalFont))
    orderInfo.addCell(Phrase("Ship Date", normalFont))
    orderInfo.addCell(Phrase(row.shipDate.toString(), normalFont))
    document.add(orderInfo)



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

