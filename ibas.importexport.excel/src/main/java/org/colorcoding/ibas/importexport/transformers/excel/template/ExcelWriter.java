package org.colorcoding.ibas.importexport.transformers.excel.template;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.colorcoding.ibas.bobas.data.DateTime;
import org.colorcoding.ibas.bobas.data.Decimal;

/**
 * excel文件写入
 * 
 * @author Niuren.Zhu
 *
 */
public class ExcelWriter extends FileWriter {

	public ExcelWriter() {
		this.setCacheRows(60);
	}

	private int cacheRows;

	public final int getCacheRows() {
		return cacheRows;
	}

	public final void setCacheRows(int cacheRows) {
		this.cacheRows = cacheRows;
	}

	private Workbook workbook;

	protected final Workbook getWorkbook() {
		return workbook;
	}

	protected final void setWorkbook(Workbook workbook) {
		this.workbook = workbook;
	}

	@Override
	public void write(File file) throws WriteFileException, IOException {
		if (file.isFile() && file.exists()) {
			file.delete();
		}
		file.getParentFile().mkdirs();
		file.createNewFile();
		SXSSFWorkbook workBook = null;
		try {
			workBook = new SXSSFWorkbook(this.getCacheRows());
			Sheet sheet = workBook.createSheet(this.getTemplate().getDescription());
			this.setWorkbook(workBook);
			this.writeHead(sheet);
			this.writeObjects(sheet);
			this.writeDatas(sheet);
			this.writeDataFormat(sheet);// 带格式空数据
			workBook.write(new FileOutputStream(file));
		} catch (Exception e) {
			throw new WriteFileException(e);
		} finally {
			if (workBook != null) {
				workBook.close();
				workBook.dispose();
				this.setWorkbook(null);
			}
		}

	}

	protected void writeHead(Sheet sheet) {
		Head head = this.getTemplate().getHead();
		CellRangeAddress range = new CellRangeAddress(head.getStartingRow(), head.getEndingRow(),
				head.getStartingColumn(), head.getEndingColumn());
		sheet.addMergedRegion(range);
		Row row = sheet.createRow(head.getStartingRow());
		row.setHeight((short) (row.getHeight() * 2));
		Cell cell = row.createCell(head.getStartingColumn());
		cell.setCellValue(head.getDescription());
		// 写入绑定信息
		CreationHelper creationHelper = this.getWorkbook().getCreationHelper();
		Drawing<?> drawing = sheet.createDrawingPatriarch();
		Comment comment = drawing.createCellComment(drawing.createAnchor(0, 0, 0, 0, head.getStartingColumn(),
				head.getStartingRow(), head.getEndingColumn(), head.getEndingRow()));
		comment.setString(creationHelper.createRichTextString(head.bindingNotes()));
		comment.setAuthor(this.getTemplate().getClass().getSimpleName());
		cell.setCellComment(comment);
	}

	protected void writeObjects(Sheet sheet) {
		CellRangeAddress range = null;
		Cell cell = null;
		Comment comment = null;
		CreationHelper creationHelper = this.getWorkbook().getCreationHelper();
		Drawing<?> drawing = sheet.createDrawingPatriarch();
		for (Object object : this.getTemplate().getObjects()) {
			range = new CellRangeAddress(object.getStartingRow(), object.getEndingRow(), object.getStartingColumn(),
					object.getEndingColumn());
			sheet.addMergedRegion(range);
		}
		Row oRow = sheet.createRow(this.getTemplate().getHead().getEndingRow() + 1);
		oRow.setHeight((short) (oRow.getHeight() * 1.2));
		Row pRow = sheet.createRow(oRow.getRowNum() + 1);
		pRow.setHeight((short) (pRow.getHeight() * 1.2));
		for (Object object : this.getTemplate().getObjects()) {
			cell = oRow.createCell(object.getStartingColumn());
			cell.setCellValue(object.getDescription());
			// 写入绑定信息
			comment = drawing.createCellComment(drawing.createAnchor(0, 0, 0, 0, object.getStartingColumn(),
					object.getStartingRow(), object.getEndingColumn(), object.getEndingRow()));
			comment.setString(creationHelper.createRichTextString(object.bindingNotes()));
			comment.setAuthor(this.getTemplate().getClass().getSimpleName());
			cell.setCellComment(comment);
			// 输出属性
			for (Property property : object.getProperties()) {
				cell = pRow.createCell(property.getStartingColumn());
				cell.setCellValue(property.getDescription());
				// 写入绑定信息
				comment = drawing.createCellComment(drawing.createAnchor(0, 0, 0, 0, property.getStartingColumn(),
						property.getStartingRow(), property.getEndingColumn(), property.getEndingRow()));
				comment.setString(creationHelper.createRichTextString(property.bindingNotes()));
				comment.setAuthor(this.getTemplate().getClass().getSimpleName());
				cell.setCellComment(comment);
			}
		}
	}

	protected void writeDatas(Sheet sheet) {

	}

	protected void writeDataFormat(Sheet sheet) {
		Cell cell = null;
		sheet.createRow(this.getTemplate().getEndingRow() + 1);
		for (Object object : this.getTemplate().getObjects()) {
			for (Property property : object.getProperties()) {
				// cell = row.createCell(property.getStartingColumn());
				if (property.getBindingClass() == DateTime.class) {
					CellStyle cellStyle = this.getWorkbook().createCellStyle();
					DataFormat format = this.getWorkbook().createDataFormat();
					cellStyle.setDataFormat(format.getFormat("yyyy-MM-dd"));
					// cell.setCellStyle(cellStyle);
				} else if (property.getBindingClass().isEnum()) {
					java.lang.Object[] constants = property.getBindingClass().getEnumConstants();
					if (constants.length <= 0) {
						continue;
					}
					String[] values = new String[constants.length];
					for (int i = 0; i < values.length; i++) {
						values[i] = constants[i].toString();
					}
					DataValidationHelper dvHelper = sheet.getDataValidationHelper();
					DataValidationConstraint dvConstraint = dvHelper.createExplicitListConstraint(values);
					CellRangeAddressList regions = new CellRangeAddressList(property.getStartingRow() + 1,
							this.getTemplate().getEndingRow() + 1, property.getStartingColumn(),
							property.getEndingColumn());
					DataValidation validation = dvHelper.createValidation(dvConstraint, regions);
					sheet.addValidationData(validation);
				} else if (property.getBindingClass() == Decimal.class) {
					// cell.setCellType(CellType.NUMERIC);
				} else if (property.getBindingClass() == Integer.class) {
					// cell.setCellType(CellType.NUMERIC);
				} else if (property.getBindingClass() == Short.class) {
					// cell.setCellType(CellType.NUMERIC);
				} else if (property.getBindingClass() == Long.class) {
					// cell.setCellType(CellType.NUMERIC);
				} else {
					// cell.setCellType(CellType.STRING);
				}
			}
		}
	}
}
