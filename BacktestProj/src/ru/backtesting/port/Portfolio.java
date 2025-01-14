package ru.backtesting.port;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import ru.backtesting.mktindicators.base.MarketIndicatorInterface;
import ru.backtesting.rebalancing.RebalancingMethod;
import ru.backtesting.rebalancing.RebalancingType;
import ru.backtesting.stockquotes.StockQuote;
import ru.backtesting.stockquotes.StockQuoteHistory;
import ru.backtesting.stockquotes.TradingTimeFrame;
import ru.backtesting.utils.Logger;
import ru.backtesting.utils.PortfolioUtils;

public class Portfolio {
	public static final String CASH_TICKER = "CASH";
	private String name;
	private int startYear;
	private int endYear;
	private final int initialAmount;
	private final RebalancingType rebalType;
	private final List<AssetAllocation> assetsAllocation;
	private LinkedHashMap<LocalDateTime, List<PositionInformation>> postionsOnDates;
    private boolean reinvestDividends = false;
    private List<MarketIndicatorInterface> timingSignals;
	private String outOfMarketPosTicker;
    private TradingTimeFrame period;
	
	public Portfolio(String name, List<AssetAllocation> assetsAllocation, int startYear, int endYear, int initialAmount,
			RebalancingType rebalancing, TradingTimeFrame period, List<MarketIndicatorInterface> timingSignals, boolean reinvestDividends) {
		super();
		this.name = name;
		this.assetsAllocation = assetsAllocation;
		this.startYear = startYear;
		this.endYear = endYear;
		this.initialAmount = initialAmount;
		this.rebalType = rebalancing;
		this.period = period;
		this.postionsOnDates = new LinkedHashMap<LocalDateTime, List<PositionInformation>>();
		this.reinvestDividends = reinvestDividends;
		this.timingSignals = timingSignals;
	}
	
	public Portfolio(String name, List<AssetAllocation> assetsAllocation, int startYear, int endYear, int initialAmount,
			RebalancingType rebalancing, TradingTimeFrame period, List<MarketIndicatorInterface> timingSignals, String outOfMarketTicker, boolean reinvestDividends) {
		super();
		this.name = name;
		this.assetsAllocation = assetsAllocation;
		this.startYear = startYear;
		this.endYear = endYear;
		this.initialAmount = initialAmount;
		this.rebalType = rebalancing;
		this.period = period;
		this.postionsOnDates = new LinkedHashMap<LocalDateTime, List<PositionInformation>>();
		this.reinvestDividends = reinvestDividends;
		
		this.timingSignals = timingSignals;
		this.outOfMarketPosTicker = outOfMarketTicker;
	}

	public void fillQuotesData() {		
		// портфель забивается данными - без расчета цены и т п
		for (AssetAllocation asset : assetsAllocation) {
			String ticker = asset.getTicker();
			
			fillQuotesData(ticker, postionsOnDates);
		}
		
		if (outOfMarketPosTicker != null && !outOfMarketPosTicker.equals(CASH_TICKER)) {
			if ( StockQuoteHistory.storage().containsDataInStorage(outOfMarketPosTicker, period) )
				return;
			
			StockQuoteHistory.storage().fillQuotesData(outOfMarketPosTicker, period);
			
			if ( !period.equals(TradingTimeFrame.Daily) )
				StockQuoteHistory.storage().fillQuotesData(outOfMarketPosTicker, TradingTimeFrame.Daily);

		}
	}
	
	private void fillQuotesData(String ticker, LinkedHashMap<LocalDateTime, List<PositionInformation>> positions) {
		if ( StockQuoteHistory.storage().containsDataInStorage(ticker, period) )
			return;
		
		StockQuoteHistory.storage().fillQuotesData(ticker, period);
		
		if ( !period.equals(TradingTimeFrame.Daily) )
			StockQuoteHistory.storage().fillQuotesData(ticker, TradingTimeFrame.Daily);


		List<LocalDateTime> dates = StockQuoteHistory.storage().getTradingDatesByFilter(ticker, period, startYear, endYear, rebalType.getFrequency());
		
		for (LocalDateTime date : dates)
			if ( positions.get(date) == null ) {
				List<PositionInformation> otherPositions = new ArrayList <PositionInformation> ();
				otherPositions.add(new PositionInformation(ticker, date));
					
				positions.put(date, otherPositions);
			}
			else {
				List<PositionInformation> otherPositions = positions.get(date);
				otherPositions.add(new PositionInformation(ticker, date));
			}
	}
	
	public void backtestPortfolio() {
	    Logger.log().info(prinfPortfolioInformation());
		
		// BuyAndHold: купил в начале срока - продал в конце
	    if (rebalType.getRebalMethod().equals(RebalancingMethod.BuyAndHold)) {			
			// купить по первой дате
			
			// currPortfolioPrice = PortfolioUtils.buyPortfolio(postionsOnDates.get(key), assetsAllocation, initialAmount, reinvestDividends);
			
			// продать по последней
			
			// разница - профит
			
			// ------------- ДОДЕЛАТЬ
	    	
	    	throw new RuntimeException("Для портфелей с типом " + rebalType.getRebalMethod() + " пока нет реализации");

	    }
	    else if (rebalType.getRebalMethod().equals(RebalancingMethod.AssetProportion)) {			
	    	LocalDateTime prevDate = null;	    	
	    	
	    	// обход по датам
	    	for (LocalDateTime date: postionsOnDates.keySet() ) {
				Logger.log().info("|| || Формирование портфеля на дату: " + date + " || ||");

				List<PositionInformation> positions = postionsOnDates.get(date);
					
    			Logger.log().info("Начинаем пересчитывать стоимость позиций в портфеле");

				double portfolioBalance = prevDate == null ? initialAmount : PortfolioUtils.calculateAllPositionsBalance(postionsOnDates.get(prevDate), period, date, reinvestDividends, true);
				
    			Logger.log().info("Пересчитали стоимость портфеля на дату [" + date + "](сколько денег у нас есть для покупки): " + Logger.log().doubleAsString(portfolioBalance));
				
	    		for (int i = 0; i < assetsAllocation.size(); i++) {
		    		String ticker = assetsAllocation.get(i).getTicker();
		    		double allocationPercent = assetsAllocation.get(i).getAllocationPercent();
		    		
		    		PositionInformation position = positions.get(i);
		    				    		
		    		// надо принять решение покупаем текущую акцию или уходим в outOfMarketTicker
		    		
		    		boolean isHoldInPortfolio = PortfolioUtils.isHoldInPortfolio(timingSignals, ticker, period, position.getTime());
		    				    		
	    			Logger.log().info("Приняли решение держать в портфеле(true)/продавать(false) [" + ticker + "] : " + isHoldInPortfolio);
		    		
		    		
		    		// для кэша все остается без изменений
		    		double quote = 0, quantity = 1;
		    				    		
		    		// если предыдущая позиция не кеш или первый прогон
		    		if ( prevDate == null || !postionsOnDates.get(prevDate).get(i).getTicker().equals(CASH_TICKER) || isHoldInPortfolio) {
		    			// купить в соответствии с assetAllocation
		    			// считаем сколько стоит акция на данный момент времени
		    			quote = StockQuoteHistory.storage().getQuoteValueByDate(ticker, period, position.getTime(), reinvestDividends);
	    			
		    			// считаем сколько мы можем купить акций по цене на данный момент времени с учетом текущей стоимости портфеля
		    			quantity = PortfolioUtils.calculateQuantityStocks(ticker, quote, portfolioBalance, assetsAllocation);
		    		
		    			// у нас есть на новые покупки quantity*quote
		    		}
		    		// если кэш
		    		else {		    			
		    			quote = allocationPercent*portfolioBalance/100;
		    			
		    			quantity = 1;
		    		}
		    		
		    		if ( isHoldInPortfolio ) {
						Logger.log().info("Купили в портфель [" + ticker + "] " + Logger.log().doubleAsString(quantity) + " лотов на сумму " + Logger.log().doubleAsString(quantity*quote) + 
								", цена лота: " + Logger.log().doubleAsString(quote) );
		    			
		    			position.buy(quantity, quantity*quote);
		    		} else { // купить в соответствии с outOfMarketTicket и других аллокаций
		    			// перекладываем текущую позицию в outOfMarketPos		    			
		    			Logger.log().info("Перекладываемся в hedge-актив " + outOfMarketPosTicker + " вместо " + ticker + " на дату " + date);
		    			
		    			PositionInformation hegdePos = new PositionInformation(outOfMarketPosTicker, date);
		    			
		    			if ( outOfMarketPosTicker.equals(CASH_TICKER)) {
			    			hegdePos.buy(1, quote*quantity);
			    			
			    			Logger.log().info("Закрыли позицию и вышли в hedge-актив [" + outOfMarketPosTicker + "] на сумму " + Logger.log().doubleAsString(quote*quantity));
		    			}
		    			else {
		    				double hedgeQuote = StockQuoteHistory.storage().getQuoteValueByDate(outOfMarketPosTicker, period, position.getTime(), reinvestDividends);
		    				
		    				double hedgeQuantity = (double) allocationPercent*portfolioBalance/hedgeQuote/100;
		    				
			    			hegdePos.buy(hedgeQuantity, hedgeQuantity*hedgeQuote);
			    			
			    			Logger.log().info("Зашли в hedge-актив [" + outOfMarketPosTicker + "]");
			    			
			    			Logger.log().info("Купили в портфель [" + outOfMarketPosTicker + "] " + Logger.log().doubleAsString(hedgeQuantity) + " лотов на сумму " + 
			    					Logger.log().doubleAsString(hedgeQuantity*hedgeQuote) + ", цена лота: " + Logger.log().doubleAsString(hedgeQuote) );
		    			}
		    			
		    			positions.set(i, hegdePos);	    			
		    		}
		    	}
	    		    		
	    		sellAllPositionWhenPortIsFull(positions);
	    		
				double newPortfolioBalance = PortfolioUtils.calculateAllPositionsBalance(positions); 
	    		
				Logger.log().info("Стоимость портфеля на [" + date + "] : " + Logger.log().doubleAsString(newPortfolioBalance));
				
    			Logger.log().info("Информация по позициям нового портфеля ниже:");
    			
    			PortfolioUtils.printPositions(postionsOnDates.get(date));
				
				Logger.log().info("-------------");
	    		
	    		prevDate = date;
	    	}
	    }
	    else if (rebalType.getRebalMethod().equals(RebalancingMethod.ForSignals)) {
	    	// вместо распределения ограничиваем риски макс позицией в том или ином инструменте - скорее риск-менеджмент, а не распределение
	    	
	    	// по frequency перекладываемся в сигналы в соответствии с риском asset alloc
	    	
	    	// если нет сигнала, то в защитный актив
	    	
	    	// мощно перекладыываться c частотой раз в день - не чаще
	    	
			// ------------- ДОДЕЛАТЬ - нужно ли
	    	
	    	throw new RuntimeException("Для портфелей с типом " + rebalType.getRebalMethod() + " пока нет реализации");
		}	    
	}
	
	private void sellAllPositionWhenPortIsFull(List<PositionInformation> positions) {		
		int firstFullIndex = -1;
		
		// ищем первую позиции с 100 процентным уровнем аллокации
		for (int i = 0; i < assetsAllocation.size(); i++) {
    		double allocationPercent = assetsAllocation.get(i).getAllocationPercent();
    		
    		if ( allocationPercent == 100) {
    			firstFullIndex = i;
    			break;
    		}
		}
		
		// продаем все кроме позиции с 100 процентным уровнем аллокации
		for (int i = 0; i < assetsAllocation.size(); i++) {    		
			PositionInformation position = positions.get(i);
    		
    		if ( firstFullIndex!= - 1 && i != firstFullIndex)
    			position.sell();
		}
	}
	
	private String prinfPortfolioInformation() {
		String inf = "";
		
		if (rebalType.getRebalMethod().equals(RebalancingMethod.BuyAndHold)) {
			inf += "Портфель типа Buy&Hold, название : " + name + "\n";
		} else if (rebalType.getRebalMethod().equals(RebalancingMethod.AssetProportion)) {
			inf += "Портфель типа TimingPortfolio с ребалансировкой активов по пропорциям, название " + name+  "\n";
			inf += "Частота ребалансировки активов: " + rebalType.getFrequency() + "\n";
			inf += "Распределение активов: " + assetsAllocation + "\n";
			inf += "Инвестирование дивидендов: " + reinvestDividends + "\n";
			
			if (outOfMarketPosTicker != null)
				inf += "Название hedge-актива при медвежьих рынках или срабатывании сигналов: " + outOfMarketPosTicker + "\n";
		} else if (rebalType.getRebalMethod().equals(RebalancingMethod.ForSignals)) {
			inf += "Портфель типа ForSignals с ребалансировкой активов по сигналам, название " + name + "\n";
			inf += "Частота ребалансировки активов: " + rebalType.getFrequency() + "\n";
			inf +="Распределение активов: " + assetsAllocation + "\n";
			inf += "Инвестирование дивидендов: " + reinvestDividends + "\n";
			
			if (outOfMarketPosTicker != null)
				inf += "Название hedge-актива при медвежьих рынках или срабатывании сигналов: " + outOfMarketPosTicker + "\n";
		}
		
		return inf;
	}
	
	@Deprecated
	private boolean haveTimingSignals() {
		return timingSignals != null && timingSignals.size() != 0;
	}
	
	public int getStartYear() {
		return startYear;
	}

	public int getEndYear() {
		return endYear;
	}

	public int getInitialAmount() {
		return initialAmount;
	}


	public RebalancingType getRebalancing() {
		return rebalType;
	}

	public List<AssetAllocation> getAssetsAllocation() {
		return assetsAllocation;
	}
	
	public void printAllPosiotions() {
		Logger.log().info("Portfolio: " + name);
		Logger.log().info("=============");
		
		for (AssetAllocation asset : assetsAllocation) {
			String ticker = asset.getTicker();
			Logger.log().info("Ticker: " + ticker + ", allocation - " + asset.getAllocationPercent() + " %");
		}
		
		for(LocalDateTime date: postionsOnDates.keySet() ) {
			Logger.log().info("date: " + date);
			
			List<PositionInformation> positions = postionsOnDates.get(date);
			
			for (PositionInformation position : positions) {
				StockQuote quote = StockQuoteHistory.storage().getQuoteByDate(position.getTicker(), period, position.getTime());
				
				Logger.log().info("____quantity:   " + position.getQuantity());
				Logger.log().info("____price: " + position.getQuantity()*quote.getClose());
				Logger.log().info("____open:   " + quote.getOpen());
				Logger.log().info("____high:   " + quote.getHigh());
				Logger.log().info("____low:    " + quote.getLow());
				Logger.log().info("____close:  " + quote.getClose());
				Logger.log().info("____adjClose:  " + quote.getAdjustedClose());
			}
			
			Logger.log().info("-------------");
		}
	}

	public String getName() {
		return name;
	}

	public LinkedHashMap<LocalDateTime, List<PositionInformation>> getPostionsOnDates() {
		return postionsOnDates;
	}

	public String getOutOfMarketPosTicker() {
		return outOfMarketPosTicker;
	}
	
	public double getFinalBalance() {
		List<LocalDateTime> portDates = Lists.newArrayList(postionsOnDates.keySet());

		int lastIndex = portDates.size() - 1;
		
		List<PositionInformation> positions = postionsOnDates.get(portDates.get(lastIndex));
		
		return PortfolioUtils.calculateAllPositionsBalance(positions);
	}
		
	public TradingTimeFrame getPeriod() {
		return period;
	}

	public Set<String> getAllTickersInPort() {
		Set<String> tickers = new HashSet<String>();
		for (int i = 0; i < assetsAllocation.size(); i++) {
    		String ticker = assetsAllocation.get(i).getTicker();
    		
    		tickers.add(ticker);
		}
		
		if ( outOfMarketPosTicker != null && !outOfMarketPosTicker.equals(CASH_TICKER) )
			tickers.add(outOfMarketPosTicker);
		
		return tickers;
	}
}
