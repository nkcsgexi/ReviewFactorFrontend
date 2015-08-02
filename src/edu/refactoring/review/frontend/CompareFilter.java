package edu.refactoring.review.frontend;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.eclipse.compare.BufferedContent;
import org.eclipse.compare.ICompareFilter;
import org.eclipse.jface.text.IRegion;

public class CompareFilter implements ICompareFilter {

	private final char Left = 'L';
	private final char Right = 'R';
	private final char Ancestor = 'A';
	private final Dictionary<String, Integer> RightLines = new Hashtable<String, Integer>();
	private final Dictionary<String, Integer> LeftLines = new Hashtable<String, Integer>();
	private final Dictionary<Integer, Collection<IRegion>> LeftRegions = new Hashtable<Integer, Collection<IRegion>>();
	private final Dictionary<Integer, Collection<IRegion>> RightRegions = new Hashtable<Integer, Collection<IRegion>>();
	private final Dictionary<Integer, Collection<IRegion>> LeftAntiRegions = new Hashtable<Integer, Collection<IRegion>>();
	private final Dictionary<Integer, Collection<IRegion>> RightAntiRegions = new Hashtable<Integer, Collection<IRegion>>();
	private final Comparator<IRegion> RegionComparator = new Comparator<IRegion>() {
		@Override
		public int compare(IRegion R1, IRegion R2) {
			return R1.getOffset() - R2.getOffset();
	}};
		
	private boolean IgnoreRegions = true;
	
	private IRegion createRegion(int offset, int length) {
		return new IRegion() {
			@Override
			public int getLength() {
				return length;
			}

			@Override
			public int getOffset() {
				return offset;
			}};
	}
	
	private Collection<IRegion> calculateAntiRegions(Collection<IRegion> Regions, String line) {
		Collection<IRegion> Results = new ArrayList<IRegion>();
		Queue<IRegion> Heap = new PriorityQueue<IRegion>(RegionComparator);
		Heap.addAll(Regions);
		for(int start = 0; start < line.length(); ) {
			IRegion Head = Heap.poll();
			if (Head == null) {
				Results.add(createRegion(start, line.length()- start));
				break;
			}
			int end = Head.getOffset();
			if (start != end) {
				Results.add(createRegion(start, end - start));
			}
			start = Head.getLength() + Head.getOffset();
		}
		return Results;
	}

	
	void populateLines(InputStream Content, Dictionary<String, Integer> table) throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(Content));
		String line;
		int lineNumber = 1;
		while(null != (line = in.readLine())) {
		   table.put(line, lineNumber ++);
		}
	}
	
	@Override
	public void setInput(Object input, Object ancestor, Object left, Object right) {
		try {
			if (left instanceof BufferedContent && right instanceof BufferedContent) { 	
				BufferedContent lContent = (BufferedContent) left;
				BufferedContent rContent = (BufferedContent) right;
				populateLines(lContent.getContents(), this.LeftLines);
				populateLines(rContent.getContents(), this.RightLines);
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
		return true;
	}
	
	private List<IRegion> getIgnoredRegions(String Line, int lineNumber, boolean isLeft) {
		List<IRegion> Regions = new ArrayList<IRegion>();
		
		
		return Regions;
	}

	@Override
	public IRegion[] getFilteredRegions(HashMap lineComparison) {
		String TL = (String) lineComparison.get(THIS_LINE);
		boolean isLeft = Left == (char)lineComparison.get(THIS_CONTRIBUTOR);
		boolean isRight = Right == (char)lineComparison.get(THIS_CONTRIBUTOR);
		if (isLeft) {
			return (this.IgnoreRegions ? this.LeftRegions : this.LeftAntiRegions).
					get(this.LeftLines.get(TL)).toArray(new IRegion[]{});
		}
		if (isRight) {
			return (this.IgnoreRegions ? this.RightRegions : this.RightAntiRegions).
					get(this.RightLines.get(TL)).toArray(new IRegion[]{});
		}
		return null;
	}

}
