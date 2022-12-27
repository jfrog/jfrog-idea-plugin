import React from 'react';
import {App, PageType, IDependencyPage} from 'jfrog-ide-webview';

function Container() {
    const [dependencyData, setDependencyData] = React.useState({} as IDependencyPage | undefined);
    const [pageType, setPageType] = React.useState(PageType.Empty as PageType);
    window.addEventListener('message', event => {
        setDependencyData(event.data.data);
        setPageType(event.data.type);
    });

    if (dependencyData?.id) {
        return (<>
            <App dependencyPageData={dependencyData} PanelType={pageType} />
            </>)

    }
    return <App PanelType={PageType.Empty} />;
}

export default Container;
