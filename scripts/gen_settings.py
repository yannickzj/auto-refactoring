import os
# from subprocess import call

def generate_appclasspath(prefix, class_path, projects_path, applib_path):
    appclasspath = ''

    for path in class_path:
        appclasspath += prefix + '/' + path + ':'

    applib = walkThroughPath(projects_path, applib_path)

    for lib in applib:
        appclasspath += prefix + '/' + lib + ':'

    appclasspath = appclasspath[:-1]

    return appclasspath

def walkThroughPath(projects_path, applib_path):
    applib = []
    applib_dict = {}
    for path in applib_path:
        for (dirpath, dirnames, filenames) in os.walk(projects_path + path):
            for filename in filenames:
                if filename.endswith('.jar'):
                    applib_dict[filename] = path

    for filename, path in applib_dict.iteritems():
        applib.append(path + '/' + filename)

    return applib

if __name__ == '__main__':

    prefix = '${env.PROJECT_DIR}'

    settings_path = '../settings/'

    projects_path = '../projects/'

    remote_settings_path = 'dc2553:/home/yannick/workspace/refactoring/settings/'

    #==========================================================
    projects = ['kafka', 'commons-collections']

    appmain_classes = ['', '']

    process_paths = [
        'kafka/streams/build/classes/java/test',
        'commons-collections/target/test-classes'
    ]

    class_paths = [
        [
            'kafka/clients/build/classes/java/main',
            'kafka/clients/build/classes/java/test',
            'kafka/core/build/classes/scala/main',
            'kafka/core/build/classes/scala/test',
            'kafka/streams/build/classes/java/main',
            #'kafka/streams/build/classes/java/test'
        ],
        [
            'commons-collections/target/classes',
            #'commons-collections/target/test-classes',
        ]
    ]

    applib_paths = [
        [
            'kafka/clients/build/dependant-libs-2.11.12',
            'kafka/core/build/dependant-libs-2.11.12',
            'kafka/streams/build/dependant-libs-2.11.12'
        ],
        [
            'commons-collections/target/lib'
        ]
    ]
    #==========================================================

    for i, project in enumerate(projects):

        # appmain
        appmain = 'appmain=' + appmain_classes[i]

        # process-dirs
        process_dirs = 'process-dirs=-process-dir ' + prefix + '/' + process_paths[i]

        # appclasspath
        appclasspath = 'appclasspath=' + generate_appclasspath(prefix, class_paths[i], projects_path, applib_paths[i])

        # generate setting file
        fout = open(settings_path + project, 'w+')
        fout.write(appclasspath + '\n\n')
        fout.write(appmain + '\n\n')
        fout.write(process_dirs)
        fout.close()

        # sync setting file
        # call(['scp', settings_path + project, remote_settings_path])
