interface Asset {
    id: number;
    symbol: string;
    name: string;
    price: number;
}

interface Props {
    asset: Asset;
    isSelected: boolean;
    onClick: () => void;
    isAdmin: boolean;
    onDelete: () => void;
}

const AssetListItem: React.FC<Props> = ({
    asset,
    isSelected,
    onClick,
    isAdmin,
    onDelete
}) => (
    <div
        className={`assets-list-item${isSelected ? " selected" : ""}`}
        onClick={onClick}
    >
        <div className="assets-list-main">
            <span className="assets-list-symbol">{asset.symbol}</span>
            <span className="assets-list-name">{asset.name}</span>
        </div>
        <div className="assets-list-details">
            <span className="assets-list-price">{asset.price.toFixed(2)} USD</span>
            {isAdmin && (
                <button className="assets-delete-btn" onClick={e => { e.stopPropagation(); onDelete(); }}>
                    Usuń
                </button>
            )}
        </div>
    </div>
);

export default AssetListItem;
