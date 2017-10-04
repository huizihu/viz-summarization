public class MaxDiff implements Distance{
	private double mdiff;
	public MaxDiff() {
		mdiff = 0;
	}
	@Override
	public double computeDistance(double [] viz1, double [] viz2) {
		assert viz1.length==viz2.length;
		for (int i =0; i<viz1.length;i++) {
			double diff = Math.abs(viz1[i]-viz2[i]);
			if (diff >mdiff){
				mdiff = diff;
			}
		}
		return mdiff;
	}
}