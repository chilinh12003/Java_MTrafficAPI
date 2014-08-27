package api.process;

public class PromotionObject
{
	/**
	 * Hình thức quảng cáo khi Vinaphone Call từ API
	 * @author Administrator
	 *
	 */
	public enum AdvertiseType
	{
		// 0 không có pormotion
		Normal(0),
		Bundle(1),
		Trial(2),
		Promotion(3),
		;

		private int value;

		private AdvertiseType(int value)
		{
			this.value = value;
		}

		public Integer GetValue()
		{
			return this.value;
		}

		public static AdvertiseType FromInt(int iValue)
		{
			for (AdvertiseType type : AdvertiseType.values())
			{
				if (type.GetValue() == iValue)
					return type;
			}
			return Normal;
		}
	}
	
	public enum BundleType
	{
		/**
		 * đăng ký gói bình thường
		 */
		Normal(0),
		/**
		 * đăng ký gói kiểu bundle (không trừ cước đăng ký, không gia hạn)
		 */
		NeverExpire(1),
		;

		private int value;

		private BundleType(int value)
		{
			this.value = value;
		}

		public Integer GetValue()
		{
			return this.value;
		}

		public static BundleType FromInt(int iValue)
		{
			for (BundleType type : BundleType.values())
			{
				if (type.GetValue() == iValue)
					return type;
			}
			return Normal;
		}
	}
	
	public enum TrialType
	{
		/**
		 * đăng ký gói bình thường
		 */
		Normal(0),
		Day(1),
		/**
		 * đăng ký gói kiểu bundle (không trừ cước đăng ký, không gia hạn)
		 */
		Week(2),
		Month(3),
		
		;

		private int value;

		private TrialType(int value)
		{
			this.value = value;
		}

		public Integer GetValue()
		{
			return this.value;
		}

		public static TrialType FromInt(int iValue)
		{
			for (TrialType type : TrialType.values())
			{
				if (type.GetValue() == iValue)
					return type;
			}
			return Normal;
		}
	}
	
	public enum PromotionType
	{
		/**
		 * đăng ký gói bình thường
		 */
		Normal(0),
		Day(1),
		/**
		 * đăng ký gói kiểu bundle (không trừ cước đăng ký, không gia hạn)
		 */
		Week(2),
		Month(3),
		
		;

		private int value;

		private PromotionType(int value)
		{
			this.value = value;
		}

		public Integer GetValue()
		{
			return this.value;
		}

		public static PromotionType FromInt(int iValue)
		{
			for (PromotionType type : PromotionType.values())
			{
				if (type.GetValue() == iValue)
					return type;
			}
			return Normal;
		}
	}
	
	public AdvertiseType mAdvertiseType = AdvertiseType.Normal;
	public BundleType mBundleType = BundleType.Normal;
	public TrialType mTrialType = TrialType.Normal;
	public PromotionType mPromotionType = PromotionType.Normal;
	
	/**
	 * Số lượng free theo (ngày, tuần, tháng) tùy thuộc vào mTrialType
	 */
	public Integer TrialNumberFree = 0;
	
	/**
	 * Số lượng free theo (ngày, tuần, tháng) tùy thuộc vào mPromotionType
	 */
	public Integer PromotionNumberFree =0;
	
}
