package edu.refactoring.review.frontend;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;

import org.eclipse.compare.BufferedContent;
import org.eclipse.compare.ICompareFilter;
import org.eclipse.jface.text.IRegion;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class CompareFilter implements ICompareFilter {

	private static final String Path = "/home/xige/Desktop/tmp.txt";
	private static final Gson G = new Gson();
	private static final char Left = 'L';
	private static final char Right = 'R';
	private static final char Ancestor = 'A';

	private int RightLineCount;
	private int LeftLineCount;

	private final Multimap<String, Integer> RightLines = HashMultimap.create();
	private final Multimap<String, Integer> LeftLines = HashMultimap.create();
	private final Map<Integer, Collection<IRegion>> LeftRegions = new Hashtable<Integer, Collection<IRegion>>();
	private final Map<Integer, Collection<IRegion>> RightRegions = new Hashtable<Integer, Collection<IRegion>>();
	private final Map<Integer, Collection<IRegion>> LeftAntiRegions = new Hashtable<Integer, Collection<IRegion>>();
	private final Map<Integer, Collection<IRegion>> RightAntiRegions = new Hashtable<Integer, Collection<IRegion>>();
	private static final Comparator<IRegion> RegionComparator = new Comparator<IRegion>() {
		@Override
		public int compare(IRegion R1, IRegion R2) {
			return R1.getOffset() - R2.getOffset();
	}};

	private boolean IgnoreRegions = true;

	public static class SerializableRegion implements IRegion {
		private final boolean Left;
		private final int LineNum;
		private final int Length;
		private final int Offset;

		public SerializableRegion(boolean Left, int LineNum, int Length, int Offset) {
			this.Length = Length;
			this.Offset = Offset;
			this.LineNum = LineNum;
			this.Left = Left;
		}

		@Override
		public int getLength() {
			return this.Length;
		}

		@Override
		public int getOffset() {
			return this.Offset;
		}

		public int getLine() {
			return this.LineNum;
		}

		public boolean isLeft() {
			return Left;
		}
	}

	public static void main(String[] args) throws Exception {
		List<IRegion> Regions = new ArrayList<IRegion>();
		Regions.add(new SerializableRegion(true, 2, 1, 14));
		Regions.add(new SerializableRegion(false, 2, 1, 14));
		outputRegions(Regions, Path);
	}

	private Collection<IRegion> calculateAntiRegions(
			Collection<IRegion> Regions, String line) {
		Collection<IRegion> Results = new ArrayList<IRegion>();
		Queue<IRegion> Heap = new PriorityQueue<IRegion>(RegionComparator);
		Heap.addAll(Regions);
		for(int start = 0; start < line.length(); ) {
			IRegion Head = Heap.poll();
			if (Head == null) {
				Results.add(new SerializableRegion(true, 0, start,
						line.length()- start));
				break;
			}
			int end = Head.getOffset();
			if (start != end) {
				Results.add(new SerializableRegion(false, 0, start,
						end - start));
			}
			start = Head.getLength() + Head.getOffset();
		}
		return Results;
	}

	private static void outputRegions(Collection<IRegion> Regions, String Path) throws Exception {
		 Files.write(G.toJson(Regions), new File(Path), Charsets.UTF_8 );
	}

	private static Collection<IRegion> inputRegions(String Path) throws Exception {
		String data = Files.toString( new File(Path), Charsets.UTF_8 );
		Type collectionType = new TypeToken<Collection<SerializableRegion>>(){}.getType();
		return G.fromJson(data, collectionType);
	}


	int populateLines(InputStream Content, Multimap<String, Integer> table) throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(Content));
		String line;
		int lineNumber = 1;
		while(null != (line = in.readLine())) {
		   table.put(line, lineNumber ++);
		}
		return lineNumber - 1;
	}

	void populateRegions(Collection<IRegion> allRegions) {
		for (IRegion R : allRegions) {
			if (R instanceof SerializableRegion) {
				SerializableRegion SR = (SerializableRegion) R;
				boolean isLeft = SR.isLeft();
				Map<Integer, Collection<IRegion>> RegionDict =
						isLeft ? this.LeftRegions : this.RightRegions;
				if (!RegionDict.containsKey(SR.getLine())) {
					RegionDict.put(SR.getLine(), new ArrayList<IRegion>());
				}
				RegionDict.get(SR.getLine()).add(SR);
			}
		}
	}

	String getTextFromLineNumber(Multimap<String, Integer> map, int Num) {
		for (Entry<String, Integer> E : map.entries()) {
			if (E.getValue() == Num)
				return E.getKey();
		}
		return null;
	}

	void populateAntiRegions(
			int Num,
			Multimap<String, Integer> Lines,
			Map<Integer, Collection<IRegion>> Regions,
			Map<Integer, Collection<IRegion>> AntiRegions) {
		for (int i = 1; i <= Num; i ++) {
			String L = getTextFromLineNumber(Lines, i);
			Collection<IRegion> CR = new ArrayList<IRegion>();
			if (Regions.containsKey(i)) {
				CR.addAll(Regions.get(i));
			}
			AntiRegions.put(i, this.calculateAntiRegions(CR, L));
		}
	}

	@Override
	public void setInput(Object input, Object ancestor, Object left, Object right) {
		try {
			if (left instanceof BufferedContent && right instanceof BufferedContent) {
				BufferedContent lContent = (BufferedContent) left;
				BufferedContent rContent = (BufferedContent) right;
				this.LeftLineCount = populateLines(lContent.getContents(), this.LeftLines);
				this.RightLineCount = populateLines(rContent.getContents(), this.RightLines);
				populateRegions(inputRegions(Path));
				populateAntiRegions(this.LeftLineCount, this.LeftLines,
						this.LeftRegions, this.LeftAntiRegions);
				populateAntiRegions(this.RightLineCount, this.RightLines,
						this.RightRegions, this.RightAntiRegions);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean isEnabledInitially() {
		return true;
	}

	@Override
	public boolean canCacheFilteredRegions() {
		return false;
	}

	private Collection<IRegion> getIgnoredRegions(String Line,
			Multimap<String, Integer> Lines,
			Map<Integer, Collection<IRegion>> Regions) {
		for (int L : Lines.get(Line)) {
			if (Regions.containsKey(L))
				return Regions.get(L);
		}

		return new ArrayList<IRegion>();
	}

	@Override
	public IRegion[] getFilteredRegions(HashMap lineComparison) {
		String TL = (String) lineComparison.get(THIS_LINE);
		boolean isLeft = Left == (char)lineComparison.get(THIS_CONTRIBUTOR);
		boolean isRight = Right == (char)lineComparison.get(THIS_CONTRIBUTOR);
		if (isLeft) {
			return getIgnoredRegions(TL, this.LeftLines, IgnoreRegions ?
					this.LeftRegions : this.LeftAntiRegions).toArray(new IRegion[]{});
		}
		if (isRight) {
			return getIgnoredRegions(TL, this.RightLines, IgnoreRegions ?
					this.RightRegions : this.RightAntiRegions).toArray(new IRegion[]{});
		}
		return null;
	}

}
