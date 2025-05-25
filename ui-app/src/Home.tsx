import { Link } from "react-router-dom";
import bullbearImg from "./assets/image.jpg";
import './App.css';
import './Home.css';

function Home() {
    return (
        <div className="home-main-container">
            <div className="home-content-wrapper">
                <div className="home-header">
                    {/* Usuń ikony byka i niedźwiedzia */}
                    <h1 className="home-title">Stock Market Simulator</h1>
                    <p className="home-subtitle">
                        Symulator giełdy, w którym możesz kupować i sprzedawać aktywa, śledzić ich ceny w czasie rzeczywistym oraz zarządzać swoim portfelem inwestycyjnym.<br />
                    </p>
                    <img
                        src={bullbearImg}
                        alt="Walka byka z niedźwiedziem - symbol giełdy"
                        className="home-bullbear-img"
                    />
                </div>
                <div className="home-assets-section">
                    <div className="home-assets-info">
                        <h3>Co możesz zrobić?</h3>
                        <ul>
                            <li>Obserwuj aktualne ceny akcji, kryptowalut i innych aktywów</li>
                            <li>Analizuj wykresy historyczne cen</li>
                            <li>Kupuj i sprzedawaj aktywa do swojego portfela</li>
                            <li>Śledź swój zysk i saldo konta na bieżąco</li>
                        </ul>
                    </div>
                    <Link to="/add-asset">
                        <button className="home-assets-btn">
                            Przeglądaj Aktywa
                        </button>
                    </Link>
                </div>
                <div className="home-side-info">
                    <div className="home-side-info-title">Dlaczego warto korzystać?</div>
                    <ul className="home-side-info-list">
                        <li>Bez ryzyka – inwestujesz wirtualne środki</li>
                        <li>Uczysz się inwestowania na realnych danych rynkowych</li>
                        <li>Możesz rywalizować z innymi użytkownikami</li>
                        <li>Przyjazny i nowoczesny interfejs</li>
                    </ul>
                    <div className="home-side-info-tip">
                        💡 Tip: Dodawaj środki i testuj różne strategie inwestycyjne bez ryzyka utraty prawdziwych pieniędzy!
                    </div>
                </div>
            </div>
            <div className="home-footer">
                <Link to="/add-user">
                    <button className="home-admin-btn">
                        Panel użytkowników (admin)
                    </button>
                </Link>
                <div className="home-footer-info">
                    <span>Panel użytkowników dostępny tylko dla administratorów.</span>
                </div>
            </div>
        </div>
    );
}

export default Home;
