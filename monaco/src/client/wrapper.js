/* --------------------------------------------------------------------------------------------
 * Copyright (c) 2024 TypeFox and others.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 * ------------------------------------------------------------------------------------------ */
import * as monaco from 'monaco-editor';
import { initServices } from 'monaco-languageclient/vscode/services';
// monaco-editor does not supply json highlighting with the json worker,
// that's why we use the textmate extension from VSCode
import getThemeServiceOverride from '@codingame/monaco-vscode-theme-service-override';
import getTextmateServiceOverride from '@codingame/monaco-vscode-textmate-service-override';
import '@codingame/monaco-vscode-theme-defaults-default-extension';
import '@codingame/monaco-vscode-json-default-extension';
import { MonacoLanguageClient } from 'monaco-languageclient';
import { WebSocketMessageReader, WebSocketMessageWriter, toSocket } from 'vscode-ws-jsonrpc';
import { CloseAction, ErrorAction } from 'vscode-languageclient';
import { useWorkerFactory } from 'monaco-editor-wrapper/workerFactory';
export const configureMonacoWorkers = () => {
    useWorkerFactory({
        ignoreMapping: true,
        workerLoaders: {
            editorWorkerService: () => new Worker(new URL('monaco-editor/esm/vs/editor/editor.worker.js', import.meta.url), { type: 'module' }),
        }
    });
};
export const runClient = async () => {
    await initServices({
        serviceConfig: {
            userServices: {
                ...getThemeServiceOverride(),
                ...getTextmateServiceOverride(),
            },
            debugLogging: true,
        }
    });
    // register language with Monaco
    monaco.languages.register({
        id: window.parent.intinoDslEditorParameters().language,
        extensions: ['.' + window.parent.intinoDslEditorParameters().language],
        aliases: [window.parent.intinoDslEditorParameters().language.toUpperCase(), window.parent.intinoDslEditorParameters().language],
    });
    // create monaco editor
    const editor = monaco.editor.create(document.getElementById('monaco-editor-root'), {
        automaticLayout: true,
        wordBasedSuggestions: 'off'
    });
    const parameters = window.parent.intinoDslEditorParameters();
    const model = monaco.editor.createModel(parameters.content, parameters.language, monaco.Uri.parse(parameters.uri));
    editor.setModel(model);
    window.parent.intinoDslEditorSetup(editor);
    initWebSocketAndStartClient(parameters.webSocketUrl);
};
/** parameterized version , support all languageId */
export const initWebSocketAndStartClient = (url) => {
    const webSocket = new WebSocket(url);
    webSocket.onopen = () => {
        const socket = toSocket(webSocket);
        const reader = new WebSocketMessageReader(socket);
        const writer = new WebSocketMessageWriter(socket);
        const languageClient = createLanguageClient({
            reader,
            writer
        });
        languageClient.start();
        reader.onClose(() => languageClient.stop());
    };
    return webSocket;
};
export const createLanguageClient = (transports) => {
    return new MonacoLanguageClient({
        name: window.parent.intinoDslEditorParameters().language + ' client',
        clientOptions: {
            // use a language id as a document selector
            documentSelector: [window.parent.intinoDslEditorParameters().language],
            // disable the default error handler
            errorHandler: {
                error: () => ({ action: ErrorAction.Continue }),
                closed: () => ({ action: CloseAction.DoNotRestart })
            }
        },
        // create a language client connection from the JSON RPC connection on demand
        connectionProvider: {
            get: () => {
                return Promise.resolve(transports);
            }
        }
    });
};
//# sourceMappingURL=wrapper.js.map