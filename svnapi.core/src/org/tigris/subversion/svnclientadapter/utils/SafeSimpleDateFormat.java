package org.tigris.subversion.svnclientadapter.utils;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class SafeSimpleDateFormat
{
    private final String _format;
    private static final ThreadLocal<Map<String, SimpleDateFormat>> _dateFormats = new ThreadLocal<Map<String, SimpleDateFormat>>()
    {
        public Map<String, SimpleDateFormat> initialValue()
        {
            return new HashMap<String, SimpleDateFormat>();
        }
    };

    private SimpleDateFormat getDateFormat(String format)
    {
        Map<String, SimpleDateFormat> formatters = _dateFormats.get();
        SimpleDateFormat formatter = formatters.get(format);
        if (formatter == null)
        {
            formatter = new SimpleDateFormat(format);
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            formatters.put(format, formatter);
        }
        return formatter;
    }

    public SafeSimpleDateFormat(String format)
    {
        _format = format;
    }

    public String format(Date date)
    {
        return getDateFormat(_format).format(date);
    }

    public String format(Object date)
    {
        return getDateFormat(_format).format(date);
    }

    public Date parse(String day) throws ParseException
    {
        return getDateFormat(_format).parse(day);
    }

    public void setTimeZone(TimeZone tz)
    {
        getDateFormat(_format).setTimeZone(tz);
    }

    public void setCalendar(Calendar cal)
    {
        getDateFormat(_format).setCalendar(cal);
    }

    public void setNumberFormat(NumberFormat format)
    {
        getDateFormat(_format).setNumberFormat(format);
    }

    public void setLenient(boolean lenient)
    {
        getDateFormat(_format).setLenient(lenient);
    }

    public void setDateFormatSymbols(DateFormatSymbols symbols)
    {
        getDateFormat(_format).setDateFormatSymbols(symbols);
    }

    public void set2DigitYearStart(Date date)
    {
        getDateFormat(_format).set2DigitYearStart(date);
    }
}