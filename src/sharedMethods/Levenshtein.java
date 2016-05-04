package sharedMethods;

import java.util.ArrayList;

public class Levenshtein {
	
	public Levenshtein() {}
	
	private static int Minimum(int a, int b, int c) 
	{
		int mi = a;
		if (b < mi) mi = b;
		if (c < mi) mi = c;
		return mi;
	}


	public int getLevenshtein(String s, String t) 
	{
		int d[][]; // matrix
		int n; // length of s
		int m; // length of t
		int i; // iterates through s
		int j; // iterates through t
		char s_i; // ith character of s
		char t_j; // jth character of t
		int cost; // cost

		// Step 1

		n = s.length();
		m = t.length();
		if (n == 0)	return m;
		if (m == 0) return n;

		d = new int[n + 1][m + 1];

		// Step 2

		for (i = 0; i <= n; i++)
			d[i][0] = i;

		for (j = 0; j <= m; j++)
			d[0][j] = j;



		for (i = 1; i <= n; i++) 
		{
			s_i = s.charAt(i - 1);
			for (j = 1; j <= m; j++) 
			{
				t_j = t.charAt(j - 1);
				if (s_i == t_j)
					cost = 0;
				else
					cost = 1;
		
				d[i][j] = Minimum(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost);
			}
		}
		return d[n][m];

	}
	
	public int getSimilarWordsNumByLevenshtein(String a, String b)
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
				int longer = x.get(i).length() > y.get(j).length() ? x.get(i).length() : y.get(j).length();
				int le = getLevenshtein(x.get(i), y.get(j));
				if( le == 0 || (le == 1 && longer > 3) || (le == 2 && longer > 7))
				{
					count ++;
					y.remove(j);
					break;
				}
			}
		}
		return count;
	}

	

}
