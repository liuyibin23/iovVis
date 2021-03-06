/*
 * Copyright © 2016-2018 The BeiDouApp Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import React from 'react';
import beidouappAceEditor from './json-form-ace-editor.jsx';
import 'brace/mode/json';
import beautify from 'js-beautify';

const js_beautify = beautify.js;

class beidouappJson extends React.Component {

    constructor(props) {
        super(props);
        this.onTidyJson = this.onTidyJson.bind(this);
    }

    onTidyJson(json) {
        return js_beautify(json, {indent_size: 4});
    }

    render() {
        return (
            <beidouappAceEditor {...this.props} mode='json' onTidy={this.onTidyJson} {...this.state}></beidouappAceEditor>
        );
    }
}

export default beidouappJson;
