import AssetListItem from "./AssetListItem";

interface Asset {
    id: number;
    symbol: string;
    name: string;
    price: number;
}

interface Props {
    assets: Asset[];
    loading: boolean;
    isAdmin: boolean;
    selectedAsset: Asset | null;
    setSelectedAsset: (asset: Asset) => void;
    deleteAsset: (id: number) => void;
}

const AssetList: React.FC<Props> = ({
    assets,
    loading,
    isAdmin,
    selectedAsset,
    setSelectedAsset,
    deleteAsset
}) => (
    <div className="assets-list-section">
        <h2 className="assets-list-title">Lista aktywów</h2>
        <div className="assets-list-info">
            <span>Wybierz aktywo, aby zobaczyć szczegóły i wykres cenowy.</span>
        </div>
        <div className="assets-list">
            {loading ? (
                <p className="assets-error">Ładowanie aktywów...</p>
            ) : assets.length > 0 ? (
                assets.map((asset) => (
                    <AssetListItem
                        key={asset.id}
                        asset={asset}
                        isSelected={selectedAsset?.id === asset.id}
                        onClick={() => setSelectedAsset(asset)}
                        isAdmin={isAdmin}
                        onDelete={() => deleteAsset(asset.id)}
                    />
                ))
            ) : (
                <p className="assets-list-empty">Brak aktywów</p>
            )}
        </div>
    </div>
);

export default AssetList;
