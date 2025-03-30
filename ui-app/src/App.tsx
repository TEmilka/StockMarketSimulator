import { Routes, Route } from "react-router-dom";
import Home from "./Home";
import Users from "./pages/Users.tsx";
import Assets from "./pages/Assets.tsx";
import UserWallet from "./pages/UserWallet.tsx";

function App() {
    return (
        <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/add-user" element={<Users />} />
            <Route path="/add-asset" element={<Assets />} />
            <Route path="/user-wallet/:userId" element={<UserWallet/>} />
        </Routes>
    );
}

export default App;
