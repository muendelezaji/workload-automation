#!/bin/bash

class_dir=bin/classes/com/arm/wlauto/uiauto
base_classes=`python -c "import os, wlauto; print os.path.join(os.path.dirname(wlauto.__file__), 'common', 'android', '*.class')"`
mkdir -p $class_dir
cp $base_classes $class_dir

ant build

if [[ -f bin/com.arm.wlauto.uiauto.reader.jar ]]; then
    cp bin/com.arm.wlauto.uiauto.reader.jar ..
fi