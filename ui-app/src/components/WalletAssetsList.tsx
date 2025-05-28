import WalletAssetItem from "./WalletAssetItem";

interface Asset {
    id: string;
    symbol: string;
    name: string;
    price: number;
    amount: number;
}

interface Props {
    assets: Asset[];
}

const WalletAssetsList: React.FC<Props> = ({ assets }) => (
    <>
        {assets.length > 0 ? (
            <div className="wallet-assets-list">
                {assets.map((asset) => (
                    <WalletAssetItem key={asset.id} asset={asset} />
                ))}
            </div>
        ) : (
            <p className="wallet-assets-empty">Brak aktywów w portfelu.</p>
        )}
    </>
);

export default WalletAssetsList;
