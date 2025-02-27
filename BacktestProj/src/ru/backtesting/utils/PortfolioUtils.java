package ru.backtesting.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import ru.backtesting.mktindicators.base.MarketIndicatorInterface;
import ru.backtesting.port.AssetAllocation;
import ru.backtesting.port.Portfolio;
import ru.backtesting.port.PositionInformation;
import ru.backtesting.stockquotes.StockQuoteHistory;
import ru.backtesting.stockquotes.TradingTimeFrame;

public class PortfolioUtils {
	public static double buyPortfolio(List<PositionInformation> positions, List<AssetAllocation> assetsAllocation, TradingTimeFrame period, double moneyAmount, boolean dividends) {
		double price = 0;
				
		for (PositionInformation position : positions) {
			String ticker = position.getTicker();
			double currentQuote = StockQuoteHistory.storage().getQuoteValueByDate(ticker, period, position.getTime(), dividends);
								
			double quantity = calculateQuantityStocks(ticker, currentQuote, moneyAmount, assetsAllocation);
			
			position.buy(quantity, currentQuote*quantity);
			
			Logger.log().info("Купили [ticker: " + position.getTicker() + "] " +Logger.log().doubleAsString(quantity) + 
					" лотов, на сумму " + Logger.log().doubleAsString(currentQuote*quantity));

			price += currentQuote*quantity;
		}
		
		Logger.log().info("Купили активов на сумму: " + Logger.log().doubleAsString(price));
		
		return price;
	}
	
	public static double calculateQuantityStocks(String ticker, double tickerQuote, 
			double portfolioBalance, List<AssetAllocation> assetsAllocation) {
		for (AssetAllocation stock : assetsAllocation)
			if (stock.getTicker().equalsIgnoreCase(ticker) )
				return (((double)stock.getAllocationPercent()/100)*portfolioBalance/tickerQuote);
		
		throw new RuntimeException("В портфеле нет актива с тикером:" + ticker);
	}
	
	public static double calculateAllPositionsBalance(List<PositionInformation> positions) {
		double sum = 0;
		
		for(PositionInformation pos : positions)
			sum += pos.getPrice();
		
		return sum;
	}
	
	public static double calculateAllPositionsBalance(List<PositionInformation> positions, TradingTimeFrame period, LocalDateTime date, boolean reinvestDividends, boolean logging) {
		double sum = 0;
		
		for(PositionInformation pos : positions) {
			if ( pos.getTicker().equals(Portfolio.CASH_TICKER) ) {
				sum += pos.getPrice();
				
				if ( logging)
					Logger.log().info("Позиция [" + pos.getTicker() + "] лотов [" + Logger.log().doubleAsString(pos.getQuantity()) + "], цена: " + Logger.log().doubleAsString(pos.getPrice()));
			}
			else {
				double quoteValue = StockQuoteHistory.storage().getQuoteValueByDate(pos.getTicker(), period, date, reinvestDividends);
				sum += pos.getQuantity()*quoteValue;
				
				if (logging)
					Logger.log().info("Позиция [" + pos.getTicker() + "] лотов [" + Logger.log().doubleAsString(pos.getQuantity()) + "], котировка [" + quoteValue + "], " + 
						"цена: " + Logger.log().doubleAsString(pos.getQuantity()*quoteValue));
			}
		}
			
		return sum;
	}
	
	public static boolean isHoldInPortfolio(List<MarketIndicatorInterface> timingSignals, String ticker, TradingTimeFrame period, LocalDateTime date) {
		boolean holdInPort = true;
		
		if ( timingSignals != null && timingSignals.size() != 0 ) {
			
			for (MarketIndicatorInterface signal : timingSignals) {
				int result = signal.testSignal(date, ticker);
				
				Logger.log().info("Для позиции портфеля с тикером " + ticker + " и датой " + date + 
						" сигнал на покупку/продажу равен: " + result + ", цена акции: " + 
						StockQuoteHistory.storage().getQuoteValueByDate(ticker, period, date, false));
				
				if (result == -1 )
				    holdInPort = false;
			}
		}
		
		return holdInPort;
	}
	
	public static void printPositions(List<PositionInformation> positions) {
		for(PositionInformation pos : positions)
			Logger.log().info(pos.toString());
	}
	

	public static double CAGRInPercent(double beginningBalance, double endingBalance, LocalDate beginningDate, LocalDate endingDate) {		
		long days = DateUtils.duration(beginningDate, endingDate).toDays();

		return CAGRInPercent(beginningBalance, endingBalance, (double) days / 365);
	}
	
	public static double CAGRInPercent(double beginningBalance, double endingBalance, double years) {
		return (Math.pow(endingBalance/beginningBalance, 1 / years) - 1)*100;
	}
}
