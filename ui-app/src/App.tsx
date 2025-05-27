import { Routes, Route } from "react-router-dom";
import Home from "./Home";
import Users from "./pages/Users.tsx";
import Assets from "./pages/Assets.tsx";
import UserWallet from "./pages/UserWallet.tsx";
import Navbar from "./components/Navbar";
import Login from "./pages/Login";
import Notifications from "./pages/Notifications";

function App() {
    return (
        <>
            <Navbar />
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/add-user" element={<Users />} />
                <Route path="/add-asset" element={<Assets />} />
                <Route path="/user-wallet/:userId" element={<UserWallet />} />
                <Route path="/login" element={<Login />} />
                <Route path="/notifications" element={<Notifications />} />
            </Routes>
        </>
    );
}

export default App;