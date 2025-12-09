import { useAuthContext } from "@/components/contexts/AuthContext";
import { useSettingsContext } from "@/components/contexts/SettingsContext";
import Page from "@/components/main/Page"
import { gameReset } from "@/services/GameApi";
import { getPushUnsupportedMessage, usePushNotifications } from "@/services/usePushNotifications";
import type { ReactNode } from "react";
import type { IconType } from "react-icons";
import { FaRegHandPointer, FaTrash } from "react-icons/fa";
import { IoNotifications } from "react-icons/io5";
import { MdVideogameAsset } from "react-icons/md";
import { RiAdminFill } from "react-icons/ri";

type OptionType = "switch" | "button";

type OptionProps = {
    title: string;
    type?: OptionType;
    value?: boolean;
    disabled?: boolean;
    icon?: IconType;
    color?: string;
    onChange?: (value: boolean) => void;
    onClick?: () => void;
    details?: string;
};

const Option = ({ title, onChange, disabled = false, type = "switch", icon: Icon, color, value, onClick, details }: OptionProps) => {
    let control;

    switch (type) {
        case "switch":
            control = (
                <label className="switch">
                    <input
                        type="checkbox"
                        disabled={disabled}
                        onChange={(e) => onChange?.(e.target.checked)}
                        checked={value}
                    />
                    <span className="slider round"></span>
                </label>
            );
            break;

        case "button":
            control = (
                <button
                    disabled={disabled}
                    className="px-3 py-1 bg-neutral-700 rounded hover:bg-neutral-600 hover:cursor-pointer hover:disabled:cursor-not-allowed"
                    style={{ color }}
                    onClick={onClick}
                >
                    {Icon ? <Icon /> : <FaRegHandPointer />}
                </button>
            );
            break;
    }


    return (
        <div className="w-full odd:bg-neutral-800 even:bg-neutral-900 p-2 flex">
            <div className="w-full flex flex-col justify-between">
                <span className="text-md flex-1 w-full">{title}</span>
                <div className="text-xs text-neutral-500 flex-1 text-left">
                    {details}
                </div>
            </div>
            {control}
        </div>
    );
};

type SettingsSectionProps = {
    icon: IconType,
    title: string,
    children?: ReactNode,
}

const SettingsSection = ({ icon: Icon, title, children }: SettingsSectionProps) => {
    return (
        <div className="bg-neutral-800 rounded-b mb-5 overflow-hidden">
            <div className="bg-amber-400 text-black flex gap-1 px-2 items-center rounded-t text-sm uppercase">
                <Icon /> <span>{title}</span>
            </div>
            <div className="flex flex-col">
                {children}
            </div>
        </div>
    )
}

const SettingsPage = () => {
    const { me, jwt, logout } = useAuthContext();
    const {
        wantsNewMessageBadges,
        setWantsNewMessageBadges,
        wantsMoreDetails,
        setWantsMoreDetails,
        setAlwaysShowMap,
        alwaysShowMap,
        debugInfo,
        setDebugInfo,
    } = useSettingsContext();
    const { subscribe, subscription, unsubscribe, isSubscribing, isSupported } = usePushNotifications();


    const handleNotificationToggle = async (enabled: boolean) => {
        try {
            if (enabled) {
                await subscribe();
            } else {
                await unsubscribe();
            }
        } catch (error) {
            console.error('Failed to toggle notifications:', error);
        }
    };

    return (
        <Page>
            <h2
                className="text-4xl mb-4"
                style={{ fontFamily: "American Captain" }}
            >
                Settings
            </h2>
            <SettingsSection title="Game" icon={MdVideogameAsset}>
                <Option title="More game details" onChange={(e) => setWantsMoreDetails(e)} value={wantsMoreDetails} />
                <Option title="Always show map" onChange={(e) => setAlwaysShowMap(e)} value={alwaysShowMap} />
                <Option title="Debug Info" onChange={(e) => setDebugInfo(e)} value={debugInfo} />
            </SettingsSection>
            <SettingsSection title="Notifications" icon={IoNotifications} >
                <Option
                    title="Receive Notifications"
                    onChange={handleNotificationToggle}
                    value={!!subscription}
                    disabled={!me || isSubscribing || !isSupported}
                    details={
                        !isSupported
                            ? getPushUnsupportedMessage()
                            : "This includes both status updates and emergency alerts. It is recommended to turn this on."
                    }
                />
                <Option title="Show New Message Badge" onChange={(e) => setWantsNewMessageBadges(e)} value={wantsNewMessageBadges} disabled={!me} />
            </SettingsSection>
            {me && me.auth && <SettingsSection title="Moderator Options" icon={RiAdminFill} >
                <Option title="Full Reset Game" onClick={() => { gameReset(true, jwt!).then(logout); }} type="button" icon={FaTrash} color="#ff7a7a" />
            </SettingsSection>}
            {debugInfo && <div className="bg-yellow-900 text-yellow-100 p-4 mb-4 rounded text-xs">
                <p>Debug Info:</p>
                <p>isSupported: {isSupported ? 'YES' : 'NO'}</p>
                <p>ServiceWorker: {'serviceWorker' in navigator ? 'YES' : 'NO'}</p>
                <p>PushManager: {'PushManager' in window ? 'YES' : 'NO'}</p>
                <p>Standalone (nav): {('standalone' in navigator && (navigator as any).standalone) ? 'YES' : 'NO'}</p>
                <p>Standalone (media): {window.matchMedia('(display-mode: standalone)').matches ? 'YES' : 'NO'}</p>
                <p>User Agent: {navigator.userAgent}</p>
            </div>}
        </Page>
    )
}

export default SettingsPage;