import { Routes, Route } from "react-router-dom";
import Home from "./Home";
import AddUser from "./pages/AddUser.tsx";
import AddAsset from "./pages/AddAsset.tsx";

function App() {
    return (
        <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/add-user" element={<AddUser />} />
            <Route path="/add-asset" element={<AddAsset />} />
        </Routes>
    );
}

export default App;
