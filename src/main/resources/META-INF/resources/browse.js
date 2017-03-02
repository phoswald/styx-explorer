function onDelete(key) {
    if(confirm('Are you sure you want to delete the key ' + key + '?') == true) {
        document.forms["f"]["delKey"].value = key;
        document.forms["f"].submit();
    }
}
