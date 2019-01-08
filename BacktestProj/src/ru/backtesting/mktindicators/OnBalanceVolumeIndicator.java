package ru.backtesting.mktindicators;

import java.time.LocalDateTime;

import ru.backtesting.mktindicators.base.MarketIndicatorInterface;
import ru.backtesting.mktindicators.base.MarketIndicatorInterval;
import ru.backtesting.mktindicators.base.MarketIndicatorType;
import ru.backtesting.mktindicators.base.MarketIndicatorsHistory;
import ru.backtesting.utils.Logger;

public class OnBalanceVolumeIndicator implements MarketIndicatorInterface {
	private MarketIndicatorInterval interval;
	
	public OnBalanceVolumeIndicator(MarketIndicatorInterval interval) {
		this.interval = interval;
	}
	
	
	@Override
	public int testSignal(LocalDateTime date, String ticker) {
		MarketIndicatorsHistory.storage().fillOBVData(ticker, getInterval());

		double rsiValue = MarketIndicatorsHistory.storage().findIndicatorValue(ticker, 0, date, getMarketIndType(), getInterval());
		
		Logger.log().info("On Balance Volume[" + getInterval() + "] on date [" + date + "]: ticker [" + ticker + "] obv = " 
				+ Logger.log().doubleLog(rsiValue));
		
		return 1;
	}

	@Override
	public MarketIndicatorType getMarketIndType() {
		return MarketIndicatorType.OBV;
	}

	@Override
	public int getTimePeriod() {
		return 0;
	}

	@Override
	public int getAdditionalTimePeriod() {
		return 0;
	}

	@Override
	public MarketIndicatorInterval getInterval() {
		return interval;
	}

}