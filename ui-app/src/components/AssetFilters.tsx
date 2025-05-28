interface Props {
    searchTerm: string;
    setSearchTerm: (v: string) => void;
    sortBy: "price" | "name" | "id";
    setSortBy: (v: "price" | "name" | "id") => void;
    sortDirection: "asc" | "desc";
    setSortDirection: (v: "asc" | "desc") => void;
    page: number;
    setPage: (v: number) => void;
    totalPages: number;
}

const AssetFilters: React.FC<Props> = ({
    searchTerm,
    setSearchTerm,
    sortBy,
    setSortBy,
    sortDirection,
    setSortDirection,
    page,
    setPage,
    totalPages
}) => (
    <>
        <div className="assets-filters">
            <input
                type="text"
                placeholder="Szukaj po nazwie lub symbolu..."
                value={searchTerm}
                onChange={(e) => { setSearchTerm(e.target.value); setPage(0); }}
                className="assets-filter-input"
            />
            <select
                value={sortBy}
                onChange={e => { setSortBy(e.target.value as "price" | "name" | "id"); setPage(0); }}
                className="assets-filter-select"
            >
                <option value="id">Domyślne</option>
                <option value="price">Cena</option>
                <option value="name">Nazwa</option>
            </select>
            <button
                onClick={() => { setSortDirection(sortDirection === "asc" ? "desc" : "asc"); setPage(0); }}
                className="assets-filter-btn"
            >
                {sortDirection === "asc" ? "↑" : "↓"}
            </button>
        </div>
        <div style={{ display: "flex", justifyContent: "center", gap: "1rem", marginBottom: "1.5rem" }}>
            <button
                className="assets-btn"
                disabled={page === 0}
                onClick={() => setPage(Math.max(0, page - 1))}
            >
                Poprzednia
            </button>
            <span style={{ color: "#fff", fontWeight: 600 }}>
                Strona {page + 1} z {totalPages}
            </span>
            <button
                className="assets-btn"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
            >
                Następna
            </button>
        </div>
    </>
);

export default AssetFilters;
