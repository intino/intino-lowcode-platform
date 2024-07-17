import React from "react";
import { withStyles } from '@material-ui/core/styles';
import AbstractIntinoFileBrowser from "../../gen/displays/AbstractIntinoFileBrowser";
import IntinoFileBrowserNotifier from "../../gen/displays/notifiers/IntinoFileBrowserNotifier";
import IntinoFileBrowserRequester from "../../gen/displays/requesters/IntinoFileBrowserRequester";
import DisplayFactory from 'alexandria-ui-elements/src/displays/DisplayFactory';
import { withSnackbar } from 'notistack';
import { ControlledTreeEnvironment, Tree } from 'react-complex-tree';
import 'react-complex-tree/lib/style-modern.css';

const styles = theme => ({});

class IntinoFileBrowser extends AbstractIntinoFileBrowser {

	constructor(props) {
		super(props);
		this.notifier = new IntinoFileBrowserNotifier(this);
		this.requester = new IntinoFileBrowserRequester(this);
		this.state = {
		    items: [],
		    focusedItem: null,
		    expandedItems: [],
		    selectedItems: [],
		};
	};

    render() {
        const id = this.props.id + "-tree";
        return (
            <ControlledTreeEnvironment
              getItemTitle={item => item.data}
              viewState={{ [id]: { focusedItem: this.state.focusedItem, expandedItems: this.state.expandedItems, selectedItems: this.state.selectedItems } }}
              canDragAndDrop={true}
              canReorderItems={true}
              onSelectItems={this.handleSelectItems.bind(this)}
              onFocusItem={this.handleFocusItem.bind(this)}
              onExpandItem={this.handleExpandItem.bind(this)}
              onCollapseItem={this.handleCollapseItem.bind(this)}
              items={this._itemsOf(this.state.items)}>
              <Tree treeId={id} rootItem="root" treeLabel={this.translate("Files")} />
            </ControlledTreeEnvironment>
        )
    };

    refresh = (items) => {
        this.setState({items: items});
    };

    _itemsOf = (items) => {
        const result = {};
        items.forEach(i => result[i.name] = this._itemOf(i));
        return result;
    };

    _itemOf = (item) => {
        return {
            index: item.name,
            data: item.name,
            isFolder: item.type == "Folder",
            children: item.children,
            canMove: true,
            canRename: true
        };
    };

    handleFocusItem = (item) => {
        this.setState({focusedItem: item.index});
    };

    handleExpandItem = (item) => {
        this.setState({expandedItems: [...this.state.expandedItems, item.index]});
    };

    handleCollapseItem = (item) => {
        this.setState({expandedItems: this.state.expandedItems.filter(expandedItemIndex => expandedItemIndex !== item.index)});
    };

    handleSelectItems = (items, treeId) => {
        if (items.length <= 0) return;
        this.requester.open(items[0]);
        this.setState({selectedItems: items});
    };

}

export default withStyles(styles, { withTheme: true })(withSnackbar(IntinoFileBrowser));
DisplayFactory.register("IntinoFileBrowser", withStyles(styles, { withTheme: true })(withSnackbar(IntinoFileBrowser)));