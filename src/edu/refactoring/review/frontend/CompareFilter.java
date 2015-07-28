package edu.refactoring.review.frontend;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.compare.ICompareFilter;
import org.eclipse.jface.text.IRegion;

public class CompareFilter implements ICompareFilter {

	private final char Left = 'L';
	private final char Right = 'R';
	private final char Ancestor = 'A';
	
	
	public CompareFilter() {
	}

	@Override
	public void setInput(Object input, Object ancestor, Object left, Object right) {
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
		return getIgnoredRegions(TL, 0, isLeft).toArray(new IRegion[]{});
	}

}
