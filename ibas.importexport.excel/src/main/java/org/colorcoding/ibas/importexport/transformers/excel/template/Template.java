package org.colorcoding.ibas.importexport.transformers.excel.template;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.colorcoding.ibas.bobas.bo.IBusinessObject;
import org.colorcoding.ibas.bobas.bo.IBusinessObjects;
import org.colorcoding.ibas.bobas.core.fields.IFieldData;
import org.colorcoding.ibas.bobas.core.fields.IManageFields;

/**
 * 模板（sheet）
 * 
 * @author Niuren.Zhu
 *
 */
public class Template extends Area {

	public Template() {
		this.setStartingRow(AREA_AUTO_REGION);
		this.setEndingRow(AREA_AUTO_REGION);
		this.setStartingColumn(AREA_AUTO_REGION);
		this.setEndingColumn(AREA_AUTO_REGION);
	}

	private Head head;

	/**
	 * 获取-模板头
	 * 
	 * @return
	 */
	public final Head getHead() {
		if (this.head == null) {
			this.head = new Head();
		}
		return head;
	}

	/**
	 * 设置-模板头
	 * 
	 * @param head
	 */
	public final void setHead(Head head) {
		this.head = head;
	}

	private List<Object> objects;

	/**
	 * 获取-模板拥有对象
	 * 
	 * @return
	 */
	public final Object[] getObjects() {
		if (this.objects == null) {
			this.objects = new ArrayList<>();
		}
		return this.objects.toArray(new Object[] {});
	}

	/**
	 * 添加-模板拥有对象
	 * 
	 * @param object
	 */
	protected final void addObject(Object object) {
		if (this.objects == null) {
			this.objects = new ArrayList<>();
		}
		this.objects.add(object);
	}

	@Override
	public String toString() {
		return String.format("{template: %s}", super.toString());
	}

	/**
	 * 解析对象，形成模板
	 * 
	 * @param bo
	 *            待解析对象
	 * @throws ParsingException
	 *             无法识别异常
	 */
	public final void resolving(IBusinessObject bo) throws ParsingException {
		try {
			this.resolvingHead(bo);
			this.resolvingObject(bo);
			// 填充模板信息
			if (this.getObjects().length > 0) {
				Object lastObject = this.getObjects()[this.getObjects().length - 1];
				if (lastObject.getProperties().length > 0) {
					Property lastProperty = lastObject.getProperties()[lastObject.getProperties().length - 1];
					this.setEndingColumn(lastProperty.getEndingColumn());
					this.setEndingRow(lastProperty.getEndingRow());
					this.getHead().setEndingColumn(this.getEndingColumn());
				}
			}
			this.setName(this.getHead().getName());
		} catch (ParsingException e) {
			throw e;
		} catch (Exception e) {
			throw new ParsingException(e);
		}
	}

	/**
	 * 解析头区域
	 * 
	 * @param bo
	 * @throws ParsingException
	 */
	protected void resolvingHead(IBusinessObject bo) throws ParsingException {
		this.getHead().setBindingClass(bo.getClass());
		this.getHead().setName(bo.getClass().getSimpleName());
	}

	/**
	 * 解析对象区域
	 * 
	 * @param bo
	 * @return
	 * @throws ParsingException
	 */
	protected void resolvingObject(IBusinessObject bo) throws ParsingException {
		// 根对象
		Object object = new Object();
		object.setStartingRow(Object.OBJECT_STARTING_ROW);
		object.setEndingRow(object.getStartingRow());
		object.setStartingColumn(
				this.getObjects().length > 0 ? this.getObjects()[this.getObjects().length - 1].getEndingColumn() + 1
						: Object.OBJECT_STARTING_COLUMN);
		object.resolving(bo);
		object.setEndingColumn(object.getStartingColumn() + object.getProperties().length - 1);
		this.addObject(object);
		// 集合对象
		IManageFields fields = (IManageFields) bo;
		for (IFieldData field : fields.getFields()) {
			if (IBusinessObjects.class.isInstance(field.getValue())) {
				IBusinessObject item = ((IBusinessObjects<?, ?>) field.getValue()).create();
				if (item instanceof IBusinessObject) {
					this.resolvingObject((IBusinessObject) item);
				}
			}
		}
	}

	/**
	 * 解析文件，形成模板
	 * 
	 * @param file
	 *            待分析文件
	 * @throws ParsingException
	 *             无法识别异常
	 */
	public final void resolving(File file) throws ParsingException {

	}

	private FileWriter writer;

	public final FileWriter getWriter() {
		if (this.writer == null) {
			this.writer = new ExcelWriter();
		}
		return writer;
	}

	public final void setWriter(FileWriter writer) {
		this.writer = writer;
	}

	/**
	 * 模板内容输出文件
	 * 
	 * @param file
	 * @throws WriteFileException
	 * @throws IOException
	 */
	public void write(File file) throws WriteFileException, IOException {
		this.getWriter().setTemplate(this);
		this.getWriter().write(file);
	}

}
