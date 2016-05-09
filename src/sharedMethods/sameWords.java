package sharedMethods;

import java.util.*;

public class sameWords {
	
	public sameWords () {}
	
	public int getSameWordsNum(String a, String b)
	{
		String[] words_a, words_b;
		ArrayList<String> x = new ArrayList<String>(); 
		ArrayList<String> y = new ArrayList<String>(); 
		if(a.length() > b.length())
		{
			words_a = a.split(" ");
			words_b = b.split(" ");
		}
		else
		{
			words_a = b.split(" ");
			words_b = a.split(" ");
		}
		
		for (String wb : words_b) 
			x.add(wb);
		for (String wa : words_a) 
			y.add(wa);
		
		int count = 0;
		for(int i = 0; i < x.size(); i++)
		{
			for(int j = 0; j < y.size(); j++)
			{
				if(x.get(i).contentEquals(y.get(j)) || x.get(i).equalsIgnoreCase(y.get(j)))
				{
					count ++;
					y.remove(j);
					break;
				}
			}
		}

		return count;
	}
	
	public double getSameWordsRatio(String a, String b)
	{
		String[] words_a, words_b;
		ArrayList<String> x = new ArrayList<String>(); 
		ArrayList<String> y = new ArrayList<String>(); 
		if(a.length() > b.length())
		{
			words_a = a.split(" ");
			words_b = b.split(" ");
		}
		else
		{
			words_a = b.split(" ");
			words_b = a.split(" ");
		}
		
		for (String wb : words_b) 
			x.add(wb);
		for (String wa : words_a) 
			y.add(wa);
		
		int count = 0;
		for(int i = 0; i < x.size(); i++)
		{
			for(int j = 0; j < y.size(); j++)
			{
				if(x.get(i).contentEquals(y.get(j)) || x.get(i).equalsIgnoreCase(y.get(j)))
				{
					count ++;
					y.remove(j);
					break;
				}
			}
		}
		
		if(x.size()==0)
			return 0;
		else
			return (double)count / (double)x.size();
	}

	public boolean isNum(String a)
	{
		if(a.length() > 4)
			return false;
		
		String[] romans = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};
		String[] robustRomans = {"l", "ll", "lll"};
		
		for(int i = 0; i < romans.length; i++)
			if(a.equalsIgnoreCase(romans[i]))
				return true;
		
		for(int i = 0; i < robustRomans.length; i++)
			if(a.equals(robustRomans[i]))
				return true;
		
		boolean allNumber = true;
		for(int i = 0; i < a.length(); i++)
		{
			char x = a.charAt(i);
			if(x < '0' || x > '9')
			{
				allNumber = false;
				break;
			}
		}
		
		return allNumber ? true : false;
	}
	
	public boolean isArabicNum(String a)
	{
		String[] words = a.split(" ");
		if(words.length <= 1)
		{
			boolean allNumber = true;
			for(int i = 0; i < a.length(); i++)
			{
				char x = a.charAt(i);
				if((x < '0' || x > '9') && x != 'o' && x != 'O' && x != 'l')
				{
					allNumber = false;
					break;
				}
			}
			return allNumber ? true : false;
		}
		else
		{
			for(int i = 0; i < words.length; i++)
				if(!isArabicNum(words[i]))
					return false;
			
			return true;
		}
	}
	
	public boolean isOrderNum(String a)
	{
		if(a.length() > 7)
			return false;
		
		if(isNum(a) || isArabicNum(a))
			return true;
		
		int digitNum = 0;
		int letterNum = 0;
		for(int i = 0; i < a.length(); i++)
		{
			char x = a.charAt(i);
			if( x >= '0' && x <= '9')
				digitNum++;
			if( x >= 'a' && x <= 'z')
				letterNum++;
			else if(x >= 'A' && x <= 'Z')
				letterNum++;
		}
		
		if(letterNum <= 1 && letterNum + digitNum < a.length())
			return true;
		
		return false;
	}
	
	public boolean isStatNum(String a)
	{
		if(isNum(a) || isArabicNum(a))
			return true;
		
		boolean haveNum = false;
		boolean haveLetter = false;
		
		for(int i = 0; i < a.length(); i++)
		{
			char x = a.charAt(i);
			if( x >= '0' && x <= '9')
				haveNum = true;
			else if( x >= 'a' && x <= 'z')
				haveLetter = true;
			else if(x >= 'A' && x <= 'Z')
				haveLetter = true;
		}
		
		if(haveNum && !haveLetter)
			return true;
		
		return false;
	}
	
	public boolean isOptionNum(String a)
	{
		String[] words = a.split(" ");
		if(words.length != 2)
			return false;
		else if(isNum(words[1]))
			return true;
		
		return false;
	}
	
	public boolean isLogicalSameWords(String a, String b)
	{
		if(a.equalsIgnoreCase(b))
			return true;
		else if(a.length() - b.length() >= 3 || a.length() - b.length() <= -3)
			return false;
		else if(a.length() < 3 || b.length() < 3)
			return false;
		else
		{
			Levenshtein l = new Levenshtein();
			if(l.getLevenshtein(a, b) <= 1 && a.length() >= 5 && b.length() >= 5)
				return true;
		}
				
		String target = "";
		String plural = "";
		if(a.length() > b.length())
		{
			target = b;
			plural = a;
		}
		else
		{
			target = a;
			plural = b;
		}
		
		if(plural.length() - target.length() == 1)
		{
			char temp = plural.charAt(plural.length()-1);
			if(temp == 's' || temp == 'S' || isSignal(temp))
				if(target.equalsIgnoreCase(plural.substring(0, plural.length()-1)))
					return true;
		}
		else if(plural.length() - target.length() == 2)
		{
			if(target.charAt(target.length()-1) == 'y' || target.charAt(target.length()-1) == 'Y')
			{
				String temp = plural.substring(plural.length()-3);
				if(temp.equalsIgnoreCase("ies"))
					if(target.substring(0, target.length()-1).equalsIgnoreCase(plural.substring(0, plural.length()-3)))
						return true;
			}
			else
			{
				String temp = plural.substring(plural.length()-2);
				if(temp.equalsIgnoreCase("es"))
					if(target.equalsIgnoreCase(plural.substring(0, plural.length()-2)))
						return true;
			}
		}
			
		
		return false;
	}
	
	public boolean isSignal(char a)
	{
		if(a < '0' || a > '9')
			if( a < 'a' || a > 'z')
				if(a < 'A' || a > 'Z')
					return true;
		return false;
	}
	
	public boolean is_aA(String a)
	{
		if(a.length() < 3)
			return false;
		
		if(a.charAt(0) > 'a' && a.charAt(0) < 'z' )
		{
			for(int i = 1; i < a.length(); i++)
			{
				if(a.charAt(i) < 'Z' && a.charAt(i) > 'A')
				{
					//LoggerSingleton.info("####" + a.charAt(i) + i);
					return true;
				}
			}
		}
		
		return false;
	}

	public boolean containTooManySameLetter(String a)
	{
		a.replace(" ", "");
		
		ArrayList<Integer> Count = new ArrayList<Integer>();
		ArrayList<Character> Char = new ArrayList<Character>();
		
		for(int i = 0; i < a.length(); i++)
		{
			char b = a.charAt(i);
			boolean match = false;
			for(int j = 0; j < Char.size(); j++)
			{
				if(b == Char.get(j))
				{
					Count.set(j, Count.get(j)+1);
					match = true;
					break;
				}
			}
			if(!match)
			{
				Char.add(b);
				Count.add(1);
			}
		}
		
		int most = 0;
		for(int i = 0; i < Count.size(); i++)
			if(Count.get(i) > most)
				most = Count.get(i);
		
		if(most*2.5 >= a.length())
		{
			if(a.length() >= 35)
				return true;
			else if(a.length() >= 25 && most * 2 >= a.length())
				return true;
			else if(a.length() >= 15 && most * 1.5 >= a.length())
				return true;
			else if(a.length() >= 4 && most * 1.25 >= a.length())
				return true;
		}
		
		return false;
	}
}
