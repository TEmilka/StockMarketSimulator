interface Asset {
    id: string;
    symbol: string;
    name: string;
    price: number;
    amount: number;
}

interface Props {
    availableAssets: Asset[];
    tradeAssetId: string;
    setTradeAssetId: (v: string) => void;
    tradeAmount: string;
    setTradeAmount: (v: string) => void;
    tradeType: "BUY" | "SELL";
    setTradeType: (v: "BUY" | "SELL") => void;
    handleTrade: (e: React.FormEvent) => void;
    selectedTradeAsset?: Asset;
    parsedTradeAmount: number;
    tradeValue: number;
}

const TradeForm: React.FC<Props> = ({
    availableAssets,
    tradeAssetId,
    setTradeAssetId,
    tradeAmount,
    setTradeAmount,
    tradeType,
    setTradeType,
    handleTrade,
    selectedTradeAsset,
    parsedTradeAmount,
    tradeValue
}) => (
    <div className="wallet-trade-section">
        <h3>Kup/Sprzedaj aktywo</h3>
        <form onSubmit={handleTrade} className="wallet-trade-form">
            <select
                value={tradeAssetId}
                onChange={e => setTradeAssetId(e.target.value)}
                required
                className="wallet-trade-select"
            >
                <option value="">Wybierz aktywo</option>
                {availableAssets.map((asset) => (
                    <option key={asset.id} value={asset.id}>
                        {asset.name} ({asset.symbol}) - {asset.price} USD
                    </option>
                ))}
            </select>
            <input
                type="number"
                placeholder="Ilość"
                value={tradeAmount}
                onChange={e => setTradeAmount(e.target.value)}
                min="0.01"
                step="0.01"
                required
                className="wallet-trade-input"
            />
            <select
                value={tradeType}
                onChange={e => setTradeType(e.target.value as "BUY" | "SELL")}
                className="wallet-trade-type"
            >
                <option value="BUY">Kup</option>
                <option value="SELL">Sprzedaj</option>
            </select>
            <button type="submit" className="wallet-trade-btn">Wykonaj</button>
        </form>
        {selectedTradeAsset && parsedTradeAmount > 0 && (
            <div className="wallet-trade-preview">
                {tradeType === "BUY" ? (
                    <span>
                        Koszt zakupu: <strong>{tradeValue.toFixed(2)} USD</strong>
                    </span>
                ) : (
                    <span>
                        Otrzymasz za sprzedaż: <strong>{tradeValue.toFixed(2)} USD</strong>
                    </span>
                )}
            </div>
        )}
    </div>
);

export default TradeForm;
