package org.tigris.subversion.subclipse.test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Currency;

import junit.framework.TestCase;

/**
 * Generic helper class for Unit tests
 */
public class TestUtils extends TestCase {

	/**
	 * Answer whether all instance members of the two objects are equal
	 * @param o1
	 * @param o2
	 * @return
	 * @throws FieldComparsonException
	 */
	public static boolean allFieldsEquals(Object o1, Object o2) throws FieldComparsonException
	{
		if (!o1.getClass().equals(o2.getClass()))
		{
			throw new FieldComparsonException("Classes are not equal !!!");
		}
		allFieldsEquals(o1, o2, o1.getClass());
		return true;
	}

	/**
	 * Answer whether all instance members of the two objects are equal
	 * @param o1
	 * @param o2
	 * @return
	 * @throws FieldComparsonException
	 */
	private static boolean allFieldsEquals(Object o1, Object o2, Class aClass) throws FieldComparsonException
	{
		if (!aClass.getSuperclass().equals(Object.class))
		{
			allFieldsEquals(o1, o2, aClass.getSuperclass());
		}

		Field[] fields = o1.getClass().getDeclaredFields(); 
		for (int i = 0; i < fields.length; i++) {
			try {
				fields[i].setAccessible(true);
				if (!Modifier.isStatic(fields[i].getModifiers()))
				{
					Object o1FieldValue = fields[i].get(o1);
					Object o2FieldValue = fields[i].get(o2);
					if ((o1FieldValue == null && o2FieldValue == null))
					{
						//Don't compare nulls 
					}
					else if ((o1FieldValue == null && o2FieldValue != null) ||
						(!o1FieldValue.equals(o2FieldValue)))
					{
						throw new FieldComparsonException(fields[i].getName() + " not equal : " + o1FieldValue + " vs. " + o2FieldValue);
					}
				}
			} catch (IllegalArgumentException e) {
				throw new FieldComparsonException(fields[i].getName() + " : " + e.toString());
			} catch (IllegalAccessException e) {
				throw new FieldComparsonException(fields[i].getName() + " : " + e.toString());
			}
		}
		return true;
	}

	public static class FieldComparsonException extends Exception
	{
	    /**
	     * @param message
	     */
	    public FieldComparsonException(String message) {
	    	super(message);
	        }
	}
	
	/** Unit test for the helper method */
	public void testAllFieldsEquals() throws Exception
	{
		Currency mock1 = Currency.getInstance("USD");		
		Currency mock2 = Currency.getInstance("EUR");
		
		assertTrue(TestUtils.allFieldsEquals(mock1, mock1));
		{
			try {
				TestUtils.allFieldsEquals(mock1, mock2);
				//Should not reach this. Execption should be raised			
				fail();
			} catch (FieldComparsonException e)
			{
				//Do nothing. We expect this expection
			}
		}
	}
}
