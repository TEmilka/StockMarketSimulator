interface Asset {
    id: string;
    symbol: string;
    name: string;
    price: number;
    amount: number;
}

interface Props {
    asset: Asset;
}

const WalletAssetItem: React.FC<Props> = ({ asset }) => (
    <div className="wallet-asset-item">
        <div className="wallet-asset-main">
            <span className="wallet-asset-symbol">{asset.symbol}</span>
            <span className="wallet-asset-name">{asset.name}</span>
        </div>
        <div className="wallet-asset-details">
            <span className="wallet-asset-amount">{asset.amount} szt.</span>
            <span className="wallet-asset-price">@ {asset.price} USD</span>
        </div>
    </div>
);

export default WalletAssetItem;
