/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.util;

import blueprint.sdk.util.StringUtil;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.CellValue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Reads Microsoft Excel 2003 file<br>
 * Requires each cells type for each sheets<br>
 * <br>
 * Example:<br>
 * int[][] cells = new int[][] { <br>
 * { XlsReader.TYPE_TEXT_SKIP, XlsReader.TYPE_TEXT, XlsReader.TYPE_NUM_TEXT }, <br>
 * { XlsReader.TYPE_TEXT_SKIP, XlsReader.TYPE_NUM_TIME, XlsReader.TYPE_NUM_DATE
 * }<br>
 * };<br>
 * XlsReader reader = new XlsReader("sample.xls", cells);<br>
 * reader.open();<br>
 * int sheetIdx = 0;<br>
 * int rowIdx = 2;<br>
 * List cellValues = reader.getCellsAt(sheetIdx, rowIdx);<br>
 * System.out.println(cellValues);<br>
 * <br>
 *
 * @author Sangmin Lee
 * @since 2009. 3. 9.
 */
@SuppressWarnings("WeakerAccess")
public class XlsReader {
    /**
     * cell type : skip
     */
    public static final int TYPE_SKIP = -1;
    /**
     * cell type : if not text, then skip whole row
     */
    public static final int TYPE_TEXT_SKIP = -2;
    /**
     * cell type : String type
     */
    public static final int TYPE_TEXT = 1;
    /**
     * cell type : Time type
     */
    public static final int TYPE_TIME = 2;
    /**
     * cell type : Date type
     */
    public static final int TYPE_DATE = 3;
    /**
     * cell type : Text/Numeric/Formula type
     */
    public static final int TYPE_NUM_TEXT = 4;
    private final String filePath;
    /**
     * each cells type for each sheet
     */
    private final int[][] cells;
    protected HSSFWorkbook wbook;
    private HSSFFormulaEvaluator evaluator;

    /**
     * Constructor
     *
     * @param filePath xls file's path
     * @param cells cells to handle (sheet*row)
     */
    public XlsReader(final String filePath, final int[][] cells) {
        this.filePath = filePath;
        this.cells = cells.clone();
    }

    public String getFilePath() {
        return filePath;
    }

    /**
     * Opens Migration Plan (*.xls) file
     *
     * @throws IOException Failed to open or read *.xls file
     */
    public void open() throws IOException {
        InputStream input = new FileInputStream(filePath);
        wbook = new HSSFWorkbook(new POIFSFileSystem(input));
        evaluator = new HSSFFormulaEvaluator(wbook);
    }

    /**
     * returns effective cells from sheetIdx starting from rowIdx, cellIdx
     *
     * @param sheetIdx index of target sheet
     * @param rowIdx index of target row
     * @return cells on target row
     * @throws IOException cell type error
     */
    public List<String> getCellsAt(final int sheetIdx, final int rowIdx) throws IOException {
        List<String> result = new ArrayList<>();

        HSSFRow row = wbook.getSheetAt(sheetIdx).getRow(rowIdx);
        int lastCell = row.getLastCellNum();
        cell_loop:
        for (int i = 0; i < cells[sheetIdx].length && i <= lastCell; i++) {
            HSSFCell cell = row.getCell(i);

            if (cell == null) {
                result.add("");
            } else {
                try {
                    switch (cells[sheetIdx][i]) {
                        case TYPE_SKIP:
                            break;
                        case TYPE_TEXT_SKIP:
                            if (cell.getCellType() == HSSFCell.CELL_TYPE_STRING) {
                                String val = cell.getRichStringCellValue().getString();
                                result.add(val == null ? "" : val);
                            } else {
                                break cell_loop;
                            }
                            break;
                        case TYPE_TEXT:
                            String textVal = cell.getRichStringCellValue().getString();
                            result.add(textVal == null ? "" : textVal);
                            break;
                        case TYPE_TIME:
                            String timeVal = getTimeValue(cell);
                            result.add(timeVal == null ? "" : timeVal);
                            break;
                        case TYPE_DATE:
                            String dateVal = getDateValue(cell);
                            result.add(dateVal == null ? "" : dateVal);
                            break;
                        case TYPE_NUM_TEXT:
                            String numTextVal = getTextValue(cell);
                            result.add(numTextVal == null ? "" : numTextVal);
                            break;
                        default:
                            throw new IOException(createTypeErrMsg(cell));
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    throw new IOException(createTypeErrMsg(cell));
                }
            }
        }

        return result;
    }

    protected String getTimeValue(final HSSFCell cell) throws IOException {
        String result;
        Calendar cal = Calendar.getInstance();

        try {
            switch (cell.getCellType()) {
                case HSSFCell.CELL_TYPE_NUMERIC:
                    cal.setTime(cell.getDateCellValue());
                    result = StringUtil.lpadZero(Integer.toString(cal.get(Calendar.HOUR_OF_DAY)), 2)
                            + StringUtil.lpadZero(Integer.toString(cal.get(Calendar.MINUTE)), 2)
                            + StringUtil.lpadZero(Integer.toString(cal.get(Calendar.SECOND)), 2);
                    break;
                default:
                    throw new IOException(createTypeErrMsg(cell));
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            throw new IOException(createTypeErrMsg(cell));
        }

        return result;
    }

    protected String getDateValue(final HSSFCell cell) throws IOException {
        String result;
        Calendar cal = Calendar.getInstance();

        try {
            switch (cell.getCellType()) {
                case HSSFCell.CELL_TYPE_NUMERIC:
                    cal.setTime(cell.getDateCellValue());
                    result = StringUtil.lpadZero(Integer.toString(cal.get(Calendar.YEAR)), 4)
                            + StringUtil.lpadZero(Integer.toString(cal.get(Calendar.MONTH) + 1), 2)
                            + StringUtil.lpadZero(Integer.toString(cal.get(Calendar.DAY_OF_MONTH)), 2);
                    break;
                default:
                    throw new IOException(createTypeErrMsg(cell));
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            throw new IOException(createTypeErrMsg(cell));
        }

        return result;
    }

    /**
     * returns numeric/formula/date/time value
     *
     * @param cell target cell
     * @return numeric value
     * @throws IOException cell type error
     */
    protected String getNumericValue(final HSSFCell cell) throws IOException {
        String result;

        try {
            switch (cell.getCellType()) {
                case HSSFCell.CELL_TYPE_NUMERIC:
                    result = Double.toString(cell.getNumericCellValue());
                    break;
                case HSSFCell.CELL_TYPE_FORMULA:
                    CellValue cellValue = evaluator.evaluate(cell);
                    switch (cellValue.getCellType()) {
                        case HSSFCell.CELL_TYPE_NUMERIC:
                            result = Long.toString((long) cellValue.getNumberValue());
                            break;
                        case HSSFCell.CELL_TYPE_BLANK:
                            result = "";
                            break;
                        default:
                            throw new IOException(createTypeErrMsg(cell));
                    }
                    break;
                default:
                    throw new IOException(createTypeErrMsg(cell));
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            throw new IOException(createTypeErrMsg(cell));
        }

        return result;
    }

    /**
     * returns numeric/string value
     *
     * @param cell target cell
     * @return text value
     * @throws IOException cell type error
     */
    protected String getTextValue(final HSSFCell cell) throws IOException {
        String result;

        try {
            switch (cell.getCellType()) {
                case HSSFCell.CELL_TYPE_NUMERIC:
                    result = Long.toString((long) cell.getNumericCellValue());
                    break;
                case HSSFCell.CELL_TYPE_FORMULA:
                    CellValue cellValue = evaluator.evaluate(cell);
                    switch (cellValue.getCellType()) {
                        case HSSFCell.CELL_TYPE_NUMERIC:
                            result = Long.toString((long) cellValue.getNumberValue());
                            break;
                        case HSSFCell.CELL_TYPE_BLANK:
                            result = "";
                            break;
                        default:
                            throw new IOException(createTypeErrMsg(cell));
                    }
                    break;
                case HSSFCell.CELL_TYPE_STRING:
                    result = cell.getRichStringCellValue().getString();
                    break;
                case HSSFCell.CELL_TYPE_BLANK:
                    result = "";
                    break;
                default:
                    throw new IOException(createTypeErrMsg(cell));
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            throw new IOException(createTypeErrMsg(cell));
        }

        return result;
    }

    /**
     * create error message for cell
     *
     * @param cell target cell
     * @return error message
     */
    protected String createTypeErrMsg(final HSSFCell cell) {
        return createTypeErrMsg(wbook.getSheetIndex(cell.getSheet()), cell.getRowIndex(), cell.getColumnIndex());
    }

    /**
     * create error message for cell at(sheetIdx, rowIdx:cellIdx)
     *
     * @param sheetIdx index of target sheet
     * @param rowIdx index of target row
     * @param cellIdx index of target cell
     * @return error message
     */
    protected String createTypeErrMsg(final int sheetIdx, final int rowIdx, final int cellIdx) {
        StringBuilder builder = new StringBuilder(256);
        builder.append("Sheet ");
        builder.append(sheetIdx + 1);
        builder.append("'s ");
        builder.append(rowIdx + 1);
        builder.append((char) ('A' + cellIdx));
        switch (cells[sheetIdx][cellIdx]) {
            case TYPE_SKIP:
                builder.append(" should be skipped");
                break;
            case TYPE_TEXT_SKIP:
                builder.append(" should be text or null(skip whole row)");
                break;
            case TYPE_TEXT:
                builder.append(" should be text");
                break;
            case TYPE_TIME:
                builder.append(" should be time");
                break;
            case TYPE_DATE:
                builder.append(" should be date");
                break;
            case TYPE_NUM_TEXT:
                builder.append(" should be text or numeric");
                break;
            default:
                builder.append(" is not defined");
                break;
        }
        builder.append(". File path is '");
        builder.append(getFilePath());
        builder.append('\'');
        return builder.toString();
    }

    protected HSSFWorkbook getWbook() {
        return wbook;
    }

    protected void setWbook(final HSSFWorkbook wbook) {
        this.wbook = wbook;
    }

    protected HSSFFormulaEvaluator getEvaluator() {
        return evaluator;
    }

    protected void setEvaluator(final HSSFFormulaEvaluator evaluator) {
        this.evaluator = evaluator;
    }
}